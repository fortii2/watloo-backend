
package me.forty2.watloo.repository;

import me.forty2.watloo.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByNameAndPostCode(String name, String postCode);
}
