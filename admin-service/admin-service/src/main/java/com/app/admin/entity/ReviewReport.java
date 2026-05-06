package com.app.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long providerId;

    @Column(nullable = false)
    private Long reportedBy;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(nullable = false)
    private String status;

    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
