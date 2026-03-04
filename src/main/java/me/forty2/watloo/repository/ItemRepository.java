package me.forty2.watloo.repository;

import me.forty2.watloo.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findAllByTelegramUserIdOrderByCreatedAtDesc(Long telegramUserId);

    List<Item> findAllByOrderByCreatedAtDesc();
}
