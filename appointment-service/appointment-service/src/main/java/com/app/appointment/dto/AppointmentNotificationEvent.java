package com.app.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentNotificationEvent {
    private Long userId;
    private Long appointmentId;
    private String type;
    private String channel;
    private String message;
    private LocalDateTime scheduledFor;
}
