package com.app.provider.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "providers")
public class Provider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String specialization;

    @Column(nullable = false)
    private String imageUrl;

    private String qualification;

    private int experienceYears;

    @Column(length = 1000)
    private String bio;

    @Column(nullable = false)
    private String clinicName;

    private String clinicAddress;

    @Column(nullable = false)
    private double avgRating;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private boolean available;

    @Column(name = "minimum_fees", nullable = false, columnDefinition = "DOUBLE DEFAULT 0.0")
    private double minimumFees;

    @Column(updatable = false, nullable = false)
    private LocalDate createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDate.now();
    }
}
