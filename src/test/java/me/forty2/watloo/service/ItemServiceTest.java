package me.forty2.watloo.service;

import me.forty2.watloo.entity.Item;
import me.forty2.watloo.exception.InvalidItemException;
import me.forty2.watloo.exception.RemoveItemException;
import me.forty2.watloo.repository.ItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ItemService itemService;

    private static final Long TELEGRAM_USER_ID = 12345L;

    @Nested
    @DisplayName("addItem success")
    class AddItemSuccess {

        @Test
        @DisplayName("saves item with name only and returns confirmation")
        void nameOnly() {
            when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
                Item i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            String result = itemService.addItem(TELEGRAM_USER_ID, "Desk Lamp", null);

            assertThat(result).contains("Item added successfully").contains("Desk Lamp");
            ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
            verify(itemRepository).save(captor.capture());
            Item saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Desk Lamp");
            assertThat(saved.getDescription()).isNull();
            assertThat(saved.getTelegramUserId()).isEqualTo(TELEGRAM_USER_ID);
        }

        @Test
        @DisplayName("saves item with name and description and returns confirmation")
        void nameAndDescription() {
            when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
                Item i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            String result = itemService.addItem(TELEGRAM_USER_ID, "Chair", "Good condition");

            assertThat(result).contains("Item added successfully").contains("Chair").contains("Good condition");
            ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
            verify(itemRepository).save(captor.capture());
            Item saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Chair");
            assertThat(saved.getDescription()).isEqualTo("Good condition");
        }

        @Test
        @DisplayName("trims name and description")
        void trimsWhitespace() {
            when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

            itemService.addItem(TELEGRAM_USER_ID, "  Bookshelf  ", "  used  ");

            ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
            verify(itemRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Bookshelf");
            assertThat(captor.getValue().getDescription()).isEqualTo("used");
        }
    }

    @Nested
    @DisplayName("addItem validation")
    class AddItemValidation {

        @Test
        @DisplayName("throws when name is null")
        void nullName() {
            assertThatThrownBy(() -> itemService.addItem(TELEGRAM_USER_ID, null, null))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("Item name is required");
        }

        @Test
        @DisplayName("throws when name is blank")
        void blankName() {
            assertThatThrownBy(() -> itemService.addItem(TELEGRAM_USER_ID, "   ", null))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("Item name is required");
        }

        @Test
        @DisplayName("throws when telegram user id is null")
        void nullTelegramUserId() {
            assertThatThrownBy(() -> itemService.addItem(null, "Lamp", null))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("User ID is required");
        }

        @Test
        @DisplayName("throws when name exceeds max length")
        void nameTooLong() {
            String longName = "a".repeat(501);
            assertThatThrownBy(() -> itemService.addItem(TELEGRAM_USER_ID, longName, null))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("500");
        }

        @Test
        @DisplayName("throws when description exceeds max length")
        void descriptionTooLong() {
            String longDesc = "b".repeat(2001);
            assertThatThrownBy(() -> itemService.addItem(TELEGRAM_USER_ID, "Item", longDesc))
                    .isInstanceOf(InvalidItemException.class)
                    .hasMessageContaining("2000");
        }
    }

    @Nested
    @DisplayName("getAllItems and list display")
    class GetAllItemsAndFormat {

        @Test
        @DisplayName("getAllItems returns empty list when repository is empty")
        void getAllItemsEmpty() {
            when(itemRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

            List<Item> result = itemService.getAllItems();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getAllItems returns items from repository")
        void getAllItemsReturnsFromRepo() {
            Item item = Item.builder().id(1L).name("Chair").telegramUserId(100L).build();
            when(itemRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(item));

            List<Item> result = itemService.getAllItems();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Chair");
        }

        @Test
        @DisplayName("formatItemList returns 'No items available.' for empty list")
        void formatEmptyList() {
            String text = itemService.formatItemList(Collections.emptyList());

            assertThat(text).isEqualTo("No items available.");
        }

        @Test
        @DisplayName("formatItemList returns 'No items available.' for null")
        void formatNullList() {
            String text = itemService.formatItemList(null);

            assertThat(text).isEqualTo("No items available.");
        }

        @Test
        @DisplayName("formatItemList shows name and description but NOT ID (public list)")
        void formatShowsNameAndDescriptionNoId() {
            Item a = Item.builder().id(5L).name("Desk").description("Wooden").condition("good").price("50").telegramUserId(1L).build();
            Item b = Item.builder().id(6L).name("Lamp").description(null).condition(null).price(null).telegramUserId(1L).build();
            String text = itemService.formatItemList(List.of(a, b));

            assertThat(text).contains("Available items (2):");
            assertThat(text).contains("1. Desk");
            assertThat(text).contains("2. Lamp");
            assertThat(text).doesNotContain("[ID: 5]");
            assertThat(text).doesNotContain("[ID: 6]");
            assertThat(text).contains("Description: Wooden");
            assertThat(text).contains("Condition: good");
            assertThat(text).contains("Price: 50");
        }

        @Test
        @DisplayName("formatItemListWithTitle with showId true shows ID for /myitems")
        void formatWithTitleShowIdShowsId() {
            Item a = Item.builder().id(5L).name("Desk").telegramUserId(1L).build();
            String text = itemService.formatItemListWithTitle(List.of(a), "Your items", true);
            assertThat(text).contains("Your items (1).");
            assertThat(text).contains("Use ID with /removeitem");
            assertThat(text).contains("[ID: 5]");
            assertThat(text).contains("1. [ID: 5] Desk");
        }

        @Test
        @DisplayName("buildItemsMessage returns message with 'No items available.' when no items")
        void buildItemsMessageEmpty() {
            when(itemRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());

            var sendMessage = itemService.buildItemsMessage(12345L);

            assertThat(sendMessage.getChatId()).isEqualTo("12345");
            assertThat(sendMessage.getText()).isEqualTo("No items available.");
        }

        @Test
        @DisplayName("buildItemsMessage returns formatted list without ID (public)")
        void buildItemsMessageWithItems() {
            Item item = Item.builder().id(3L).name("Table").description("Ikea").telegramUserId(1L).build();
            when(itemRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(item));

            var sendMessage = itemService.buildItemsMessage(999L);

            assertThat(sendMessage.getChatId()).isEqualTo("999");
            assertThat(sendMessage.getText()).contains("Available items (1):");
            assertThat(sendMessage.getText()).contains("1. Table");
            assertThat(sendMessage.getText()).contains("Description: Ikea");
            assertThat(sendMessage.getText()).doesNotContain("[ID: 3]");
        }

        @Test
        @DisplayName("formatItemList truncates when over Telegram message limit")
        void formatTruncatesWhenOverLimit() {
            Item item = Item.builder()
                    .name("X")
                    .description("d")
                    .condition("c")
                    .price("p")
                    .telegramUserId(1L)
                    .build();
            List<Item> many = Collections.nCopies(500, item);

            String text = itemService.formatItemList(many);

            assertThat(text).endsWith("(list truncated to fit message limit)");
            assertThat(text.length()).isLessThanOrEqualTo(4096 + 50);
        }
    }

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("deletes own item and returns confirmation")
        void removeOwnItem() {
            Item item = Item.builder().id(10L).name("Chair").telegramUserId(TELEGRAM_USER_ID).build();
            when(itemRepository.findById(10L)).thenReturn(Optional.of(item));

            String result = itemService.removeItem(TELEGRAM_USER_ID, 10L);

            assertThat(result).isEqualTo("Item removed successfully.");
            verify(itemRepository).deleteById(10L);
        }

        @Test
        @DisplayName("throws when item not found")
        void itemNotFound() {
            when(itemRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.removeItem(TELEGRAM_USER_ID, 999L))
                    .isInstanceOf(RemoveItemException.class)
                    .hasMessageContaining("Item not found");
            verify(itemRepository).findById(999L);
            verify(itemRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws when user does not own the item (unauthorized)")
        void unauthorizedDeletion() {
            Item item = Item.builder().id(5L).name("Desk").telegramUserId(99999L).build();
            when(itemRepository.findById(5L)).thenReturn(Optional.of(item));

            assertThatThrownBy(() -> itemService.removeItem(TELEGRAM_USER_ID, 5L))
                    .isInstanceOf(RemoveItemException.class)
                    .hasMessageContaining("You can only remove your own items");
            verify(itemRepository).findById(5L);
            verify(itemRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws when item ID is null")
        void nullItemId() {
            assertThatThrownBy(() -> itemService.removeItem(TELEGRAM_USER_ID, null))
                    .isInstanceOf(RemoveItemException.class)
                    .hasMessageContaining("Item ID is required");
            verify(itemRepository, never()).deleteById(any());
        }
    }
}
