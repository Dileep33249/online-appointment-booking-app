package com.app.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationRequest {
    @NotNull
    private Long appointmentId;
    @NotNull
    private Long patientId;
    @NotNull
    private Long providerId;
    @NotBlank
    private String mode;
    private String meetingLink;
    private String symptoms;
    private String notes;
}
