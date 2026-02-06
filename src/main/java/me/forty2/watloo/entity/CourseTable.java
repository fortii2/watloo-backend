package me.forty2.watloo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class CourseTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    // ECE
    private String subject;

    // 650
    private Integer subjectNumber;

    // 1261 for Winter 2026
    private String termCode;

    private String location;

    private String prof;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;
}
