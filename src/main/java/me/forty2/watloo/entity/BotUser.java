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
public class BotUser {
    @Id
    private Long id;

    private String firstName;

    private String lastName;

    private String username;

    private String languageCode;

    @CreationTimestamp
    @Column(updatable = false)
    private Timestamp createAt;
}
