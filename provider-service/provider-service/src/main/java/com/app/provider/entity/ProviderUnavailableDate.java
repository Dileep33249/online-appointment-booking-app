package com.app.provider.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "provider_unavailable_dates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider_id", "date"})
)
public class ProviderUnavailableDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(nullable = false)
    private LocalDate date;

    private String reason;
}
