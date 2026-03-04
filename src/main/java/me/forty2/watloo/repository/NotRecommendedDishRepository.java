package me.forty2.watloo.repository;

import me.forty2.watloo.entity.NotRecommendedDish;
import me.forty2.watloo.entity.Review;
import me.forty2.watloo.entity.dish;
import me.forty2.watloo.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface NotRecommendedDishRepository extends JpaRepository<NotRecommendedDish, Long> {

    // 防止同一条 review 重复不推荐同一道菜（可选）
    Optional<NotRecommendedDish> findByReviewAndDish(Review review, dish dish);

    // 查某条 review 不推荐了哪些菜
    List<NotRecommendedDish> findByReview(Review review);

    // 查某家餐厅的所有不推荐菜
    @Query("SELECT nd FROM NotRecommendedDish nd JOIN FETCH nd.dish d WHERE d.restaurant = :restaurant")
    List<NotRecommendedDish> findByRestaurant(Restaurant restaurant);
}