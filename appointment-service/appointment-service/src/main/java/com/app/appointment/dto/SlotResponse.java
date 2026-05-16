package com.app.appointment.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponse implements Serializable {
    private static final long serialVersionUID = 1L;
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
