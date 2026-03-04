
package me.forty2.watloo.repository;

import me.forty2.watloo.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByNameAndPostCode(String name, String postCode);
    
    // 按平均评分降序排列
    List<Restaurant> findAllByOrderByAvgRatingDescReviewCountDesc();
    
    // 查询有评论的餐厅
    @Query("SELECT r FROM Restaurant r WHERE r.reviewCount > 0 ORDER BY r.avgRating DESC, r.reviewCount DESC")
    List<Restaurant> findTopRankedRestaurants();
    List<Restaurant> findTop10ByNameContainingIgnoreCaseOrderByAvgRatingDescReviewCountDesc(String namePart);

    List<Restaurant> findTop10ByPostCodeContainingIgnoreCaseOrderByAvgRatingDescReviewCountDesc(String postCodePart);
}
