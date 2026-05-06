package com.app.notification.dto;

import com.app.notification.entity.NotificationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long userId;
    private Long appointmentId;
    private String type;
    private String channel;
    private String message;
    private NotificationStatus status;
    private LocalDateTime scheduledFor;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
