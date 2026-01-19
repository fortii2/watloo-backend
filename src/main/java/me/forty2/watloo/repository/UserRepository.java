package me.forty2.watloo.repository;

import me.forty2.watloo.entity.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<BotUser, Long> {
}
