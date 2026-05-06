package com.app.provider.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String imageUrl;
    private String specialization;
    private String qualification;
    private int experienceYears;
    private String bio;
    private String clinicName;
    private String clinicAddress;
    private double avgRating;
    private double minimumFees;
    private boolean verified;
    private boolean available;
    private LocalDate createdAt;
}
