package me.forty2.watloo.repository;

import me.forty2.watloo.entity.dish;
import me.forty2.watloo.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface DishRepository extends JpaRepository<dish, Long> {

    // 同一家餐厅下，按菜名查菜（避免重复建菜）
    Optional<dish> findByRestaurantAndDishName(Restaurant restaurant, String dishName);

    // 查某家餐厅的所有菜
    List<dish> findByRestaurant(Restaurant restaurant);
}
