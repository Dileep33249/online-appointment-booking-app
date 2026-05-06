package com.app.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private Long userId;
    private Long appointmentId;
    @NotBlank
    private String type;
    @NotBlank
    private String channel;
    @NotBlank
    private String message;
    private LocalDateTime scheduledFor;
}
