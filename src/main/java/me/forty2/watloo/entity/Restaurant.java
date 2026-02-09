package me.forty2.watloo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "restaurant")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Long restaurantId;

    
    @Column(name = "avg_rating")
    private Double avgRating;

    
    @Column(name = "review_count")
    private Integer reviewCount;

    
    @Column(nullable = false)
    private String name;

    @Column(name = "post_code")
    private String postCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.reviewCount == null) {
            this.reviewCount = 0;
        }
        
    }
}