package me.forty2.watloo.service;

import lombok.RequiredArgsConstructor;
import me.forty2.watloo.dto.ItemRegisterDTO;
import me.forty2.watloo.entity.BotUser;
import me.forty2.watloo.entity.Item;
import me.forty2.watloo.enums.UserState;
import me.forty2.watloo.exception.InvalidItemException;
import me.forty2.watloo.exception.RemoveItemException;
import me.forty2.watloo.repository.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;

    private final Map<Long, ItemRegisterDTO> itemRegisterCache = new HashMap<>();

    private static final int MAX_NAME_LENGTH = 500;
    private static final int MAX_CONDITION_LENGTH = 100;
    private static final int MAX_PRICE_LENGTH = 50;
    /** Telegram message length limit */
    private static final int TELEGRAM_MESSAGE_LIMIT = 4096;

    /**
     * Start add-item flow: set state and prompt for name.
     */
    public SendMessage startAddItem(Long chatId, User user) {
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_ITEM_NAME);
        userService.save(botUser);

        itemRegisterCache.put(user.getId(), ItemRegisterDTO.builder()
                .telegramUserId(user.getId())
                .build());

        return SendMessage.builder()
                .chatId(chatId)
                .text("Please enter the item name:")
                .build();
    }

    /**
     * After user enters name, ask for condition.
     */
    public SendMessage handleItemName(Long chatId, String messageText, User user) {
        String name = messageText == null ? "" : messageText.trim();
        if (name.isBlank()) {
            return SendMessage.builder().chatId(chatId).text("Name cannot be empty. Please enter the item name:").build();
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return SendMessage.builder().chatId(chatId).text("Name must be at most " + MAX_NAME_LENGTH + " characters. Please try again:").build();
        }

        ItemRegisterDTO dto = itemRegisterCache.get(user.getId());
        if (dto == null) {
            return sendItemFlowReset(chatId, user, "Session expired. Please send /additem again to start.");
        }
        dto.setName(name);

        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_ITEM_CONDITION);
        userService.save(botUser);

        return SendMessage.builder()
                .chatId(chatId)
                .text("Please enter the condition (e.g. new / like new / good):")
                .build();
    }

    /**
     * After user enters condition, ask for price.
     */
    public SendMessage handleItemCondition(Long chatId, String messageText, User user) {
        String condition = messageText == null ? "" : messageText.trim();
        if (condition.length() > MAX_CONDITION_LENGTH) {
            return SendMessage.builder().chatId(chatId).text("Condition must be at most " + MAX_CONDITION_LENGTH + " characters. Please try again:").build();
        }

        ItemRegisterDTO dto = itemRegisterCache.get(user.getId());
        if (dto == null) {
            return sendItemFlowReset(chatId, user, "Session expired. Please send /additem again to start.");
        }
        dto.setCondition(condition.isEmpty() ? null : condition);

        BotUser botUser = userService.getOne(user);
        botUser.setUserState(UserState.AWAITING_ITEM_PRICE);
        userService.save(botUser);

        return SendMessage.builder()
                .chatId(chatId)
                .text("Please enter the price (e.g. 100 or negotiable):")
                .build();
    }

    /**
     * After user enters price, save item and finish flow.
     */
    @Transactional
    public SendMessage handleItemPrice(Long chatId, String messageText, User user) {
        String price = messageText == null ? "" : messageText.trim();
        if (price.length() > MAX_PRICE_LENGTH) {
            return SendMessage.builder().chatId(chatId).text("Price must be at most " + MAX_PRICE_LENGTH + " characters. Please try again:").build();
        }

        ItemRegisterDTO dto = itemRegisterCache.get(user.getId());
        if (dto == null) {
            return sendItemFlowReset(chatId, user, "Session expired. Please send /additem again to start.");
        }
        dto.setPrice(price.isEmpty() ? null : price);

        Item item = Item.builder()
                .name(dto.getName())
                .description(null)
                .condition(dto.getCondition())
                .price(dto.getPrice())
                .telegramUserId(dto.getTelegramUserId())
                .build();
        itemRepository.save(item);

        itemRegisterCache.remove(user.getId());
        BotUser botUser = userService.getOne(user);
        botUser.setUserState(null);
        userService.save(botUser);

        String summary = dto.getName();
        if (dto.getCondition() != null) summary += ", condition: " + dto.getCondition();
        if (dto.getPrice() != null) summary += ", price: " + dto.getPrice();
        return SendMessage.builder()
                .chatId(chatId)
                .text("Item added successfully: " + summary)
                .build();
    }

    private SendMessage sendItemFlowReset(Long chatId, User user, String message) {
        itemRegisterCache.remove(user.getId());
        BotUser botUser = userService.getOne(user);
        if (botUser != null && (botUser.getUserState() == UserState.AWAITING_ITEM_NAME
                || botUser.getUserState() == UserState.AWAITING_ITEM_CONDITION
                || botUser.getUserState() == UserState.AWAITING_ITEM_PRICE)) {
            botUser.setUserState(null);
            userService.save(botUser);
        }
        return SendMessage.builder().chatId(chatId).text(message).build();
    }

    /**
     * Removes an item if it exists and belongs to the given Telegram user.
     *
     * @param telegramUserId the Telegram user ID (must own the item)
     * @param itemId         the item ID to remove
     * @return confirmation message
     * @throws RemoveItemException if item not found or user does not own the item
     */
    @Transactional
    public String removeItem(Long telegramUserId, Long itemId) {
        if (itemId == null) {
            throw new RemoveItemException("Item ID is required. Usage: /removeitem <id>");
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RemoveItemException("Item not found."));
        if (!item.getTelegramUserId().equals(telegramUserId)) {
            throw new RemoveItemException("You can only remove your own items.");
        }
        itemRepository.deleteById(itemId);
        return "Item removed successfully.";
    }

    /**
     * Returns all items from the database, newest first.
     */
    public List<Item> getAllItems() {
        return itemRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Builds the message to display the item list for the /items command.
     * Empty list returns "No items available."; output is truncated to Telegram message limit.
     */
    public SendMessage buildItemsMessage(Long chatId) {
        List<Item> items = getAllItems();
        String text = formatItemList(items);
        return SendMessage.builder().chatId(chatId).text(text).build();
    }

    /**
     * Formats the public item list for /items. Does NOT show item ID so others cannot attempt to remove your items.
     */
    String formatItemList(List<Item> items) {
        return formatItemListWithTitle(items, "Available items", false);
    }

    /**
     * Formats a list of items. When showId is true (e.g. /myitems), shows ID for /removeitem; when false (/items), hides ID.
     */
    String formatItemListWithTitle(List<Item> items, String title, boolean showId) {
        if (items == null || items.isEmpty()) {
            return "No items available.";
        }
        StringBuilder sb = new StringBuilder();
        String titleLine = showId
                ? title + " (" + items.size() + "). Use ID with /removeitem <id>\n---\n"
                : title + " (" + items.size() + "):\n---\n";
        sb.append(titleLine);
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            String idPart = showId && item.getId() != null ? "[ID: " + item.getId() + "] " : "";
            String block = (i + 1) + ". " + idPart + item.getName() + "\n"
                    + "Description: " + (item.getDescription() != null && !item.getDescription().isBlank() ? item.getDescription() : "-") + "\n"
                    + "Condition: " + (item.getCondition() != null && !item.getCondition().isBlank() ? item.getCondition() : "-")
                    + " | Price: " + (item.getPrice() != null && !item.getPrice().isBlank() ? item.getPrice() : "-")
                    + "\n---\n";
            if (sb.length() + block.length() > TELEGRAM_MESSAGE_LIMIT) {
                sb.append("(list truncated to fit message limit)");
                break;
            }
            sb.append(block);
        }
        return sb.toString().trim();
    }

    /**
     * Builds the message for /myitems: only items posted by the given user, with IDs so only you can /removeitem.
     */
    public SendMessage buildMyItemsMessage(Long chatId, Long telegramUserId) {
        List<Item> items = itemRepository.findAllByTelegramUserIdOrderByCreatedAtDesc(telegramUserId);
        String text = formatItemListWithTitle(items, "Your items", true);
        return SendMessage.builder().chatId(chatId).text(text).build();
    }

    // ----- 保留单行添加接口，供需要时使用 -----
    @Transactional
    public String addItem(Long telegramUserId, String name, String description) {
        if (telegramUserId == null) throw new InvalidItemException("User ID is required.");
        if (name == null || name.isBlank()) throw new InvalidItemException("Item name is required.");
        String trimmedName = name.trim();
        if (trimmedName.length() > MAX_NAME_LENGTH) throw new InvalidItemException("Item name must be at most " + MAX_NAME_LENGTH + " characters.");
        String trimmedDesc = description != null ? description.trim() : "";
        if (trimmedDesc.length() > 2000) throw new InvalidItemException("Description must be at most 2000 characters.");

        Item item = Item.builder()
                .name(trimmedName)
                .description(trimmedDesc.isEmpty() ? null : trimmedDesc)
                .telegramUserId(telegramUserId)
                .build();
        itemRepository.save(item);
        return "Item added successfully: " + trimmedName + (trimmedDesc.isEmpty() ? "" : " — " + trimmedDesc);
    }
}
