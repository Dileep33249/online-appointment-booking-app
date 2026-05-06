package com.app.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {
    private Long userId;
    private Long appointmentId;
    private String type;
    private String channel;
    private String message;
    private LocalDateTime scheduledFor;
}
