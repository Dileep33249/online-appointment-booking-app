package com.app.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {
    @NotNull
    private Long userId;
    private Long appointmentId;
    @NotBlank
    private String channel;
    @NotBlank
    private String message;
    private LocalDateTime scheduledFor;
}
