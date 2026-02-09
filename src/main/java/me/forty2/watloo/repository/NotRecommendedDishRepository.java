package me.forty2.watloo.repository;

import me.forty2.watloo.entity.NotRecommendedDish;
import me.forty2.watloo.entity.Review;
import me.forty2.watloo.entity.dish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface NotRecommendedDishRepository extends JpaRepository<NotRecommendedDish, Long> {

    // 防止同一条 review 重复不推荐同一道菜（可选）
    Optional<NotRecommendedDish> findByReviewAndDish(Review review, dish dish);

    // 查某条 review 不推荐了哪些菜
    List<NotRecommendedDish> findByReview(Review review);
}