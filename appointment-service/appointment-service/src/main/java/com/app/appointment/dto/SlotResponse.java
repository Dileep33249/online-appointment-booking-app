package com.app.appointment.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponse {
    private Long id;
    private Long providerId;
    private String doctorName;
    private String specialization;
    private String clinicName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isBooked;
    private double minimumFees;
}
