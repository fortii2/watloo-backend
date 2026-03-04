package me.forty2.watloo.repository;

import me.forty2.watloo.entity.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("saves item and finds by telegram user id ordered by created at desc")
    void saveAndFindByTelegramUserId() {
        Long userId = 100L;
        Item first = Item.builder()
                .name("First Item")
                .description("First desc")
                .telegramUserId(userId)
                .build();
        Item second = Item.builder()
                .name("Second Item")
                .telegramUserId(userId)
                .build();

        itemRepository.save(first);
        itemRepository.save(second);

        List<Item> found = itemRepository.findAllByTelegramUserIdOrderByCreatedAtDesc(userId);
        assertThat(found).hasSize(2);
        assertThat(found.get(0).getName()).isEqualTo("Second Item");
        assertThat(found.get(1).getName()).isEqualTo("First Item");
        assertThat(found.get(0).getId()).isNotNull();
        assertThat(found.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("persists item with required fields only")
    void persistWithNameAndTelegramUserIdOnly() {
        Item item = Item.builder()
                .name("Minimal Item")
                .telegramUserId(200L)
                .build();
        Item saved = itemRepository.save(item);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Minimal Item");
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getTelegramUserId()).isEqualTo(200L);
        assertThat(saved.getCreatedAt()).isNotNull();

        Item loaded = itemRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getName()).isEqualTo("Minimal Item");
        assertThat(loaded.getTelegramUserId()).isEqualTo(200L);
    }
}
