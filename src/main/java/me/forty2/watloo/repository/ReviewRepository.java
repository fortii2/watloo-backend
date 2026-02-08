package me.forty2.watloo.repository;

import me.forty2.watloo.entity.Review;
import me.forty2.watloo.entity.Restaurant;
import me.forty2.watloo.entity.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByRestaurant(Restaurant restaurant);

    List<Review> findByUser(BotUser user);
}