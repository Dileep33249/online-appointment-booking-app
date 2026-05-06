package com.app.consultation.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationResponse {
    private Long id;
    private Long appointmentId;
    private Long patientId;
    private Long providerId;
    private String mode;
    private String status;
    private String meetingLink;
    private String symptoms;
    private String diagnosis;
    private String notes;
    private String prescriptionSummary;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
