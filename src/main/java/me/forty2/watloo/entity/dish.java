package me.forty2.watloo.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "dish")
@Data
public class dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dish_id")
    private Long dishId;

    // 外键 → restaurant.restaurant_id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "dish_name", nullable = false)
    private String dishName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 被推荐次数
    @Column(name = "recommend_count", nullable = false)
    private Integer recommendCount;

    // 被不推荐次数
    @Column(name = "not_recommend_count", nullable = false)
    private Integer notRecommendCount;

    @PrePersist
    public void prePersist() {
        if (recommendCount == null) {
            recommendCount = 0;
        }
        if (notRecommendCount == null) {
            notRecommendCount = 0;
        }
    }
}
