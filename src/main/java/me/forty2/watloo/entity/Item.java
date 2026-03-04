package me.forty2.watloo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(length = 2000)
    private String description;

    /** 成色，如：全新/九成新/八成新 */
    @Column(length = 100)
    private String condition;

    /** 标价，如：100 或 面议 */
    @Column(length = 50)
    private String price;

    @Column(nullable = false)
    private Long telegramUserId;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createdAt;
}
