package com.app.appointment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Long id;
    private Long userId;
    private String patientName;
    private String doctorName;
    private Long slotId;
    private Long providerId;
    private double providerFee;
    private String status;
    private String notes;
    private Integer queueNumber;
    private LocalDateTime slotStartTime;
    private LocalDateTime slotEndTime;
    private LocalDateTime estimatedStartTime;
    private LocalDateTime estimatedEndTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
