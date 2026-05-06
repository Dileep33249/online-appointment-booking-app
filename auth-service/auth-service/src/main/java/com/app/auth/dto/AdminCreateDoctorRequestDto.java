package com.app.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCreateDoctorRequestDto {

    @NotBlank(message = "Full name is required")
    private String fullname;

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotBlank(message = "Clinic name is required")
    private String clinicName;

    @NotBlank(message = "Profile image is required")
    private String imageUrl;

    private String qualification;
    private Integer experienceYears;
    private String bio;
    private String clinicAddress;
    private Double minimumFees;
}
