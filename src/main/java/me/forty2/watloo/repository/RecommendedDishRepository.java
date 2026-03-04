package me.forty2.watloo.repository;


import me.forty2.watloo.entity.RecommendedDish;
import me.forty2.watloo.entity.Review;
import me.forty2.watloo.entity.dish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface RecommendedDishRepository extends JpaRepository<RecommendedDish, Long> {

    // 防止同一条 review 重复推荐同一道菜（可选）
    Optional<RecommendedDish> findByReviewAndDish(Review review, dish dish);

    // 查某条 review 推荐了哪些菜
    List<RecommendedDish> findByReview(Review review);
}