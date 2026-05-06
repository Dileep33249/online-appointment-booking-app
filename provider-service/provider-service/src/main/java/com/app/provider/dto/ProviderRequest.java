package com.app.provider.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderRequest {
    private Long userId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotBlank(message = "Profile image is required")
    private String imageUrl;

    private String qualification;
    private int experienceYears;
    private String bio;

    @NotBlank(message = "Clinic name is required")
    private String clinicName;

    private String clinicAddress;
    private double minimumFees;
}
