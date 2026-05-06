package com.app.notification.service;

import com.app.notification.dto.NotificationRequest;
import com.app.notification.dto.NotificationResponse;
import com.app.notification.dto.CreateNotificationRequest;
import com.app.notification.entity.Notification;
import com.app.notification.entity.NotificationStatus;
import com.app.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationResponse createBookingConfirmation(NotificationRequest request) {
        return saveAndMap("BOOKING_CONFIRMATION", request, NotificationStatus.SENT, LocalDateTime.now());
    }

    public NotificationResponse createCancellationNotification(NotificationRequest request) {
        return saveAndMap("CANCELLATION", request, NotificationStatus.SENT, LocalDateTime.now());
    }

    public NotificationResponse scheduleReminder(NotificationRequest request) {
        return saveAndMap("REMINDER", request, NotificationStatus.PENDING, null);
    }

    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::map).toList();
    }

    public List<NotificationResponse> dispatchDueReminders() {
        List<Notification> dueNotifications = notificationRepository.findByStatusAndScheduledForBefore(
                NotificationStatus.PENDING, LocalDateTime.now().plusMinutes(1)
        );
        dueNotifications.forEach(notification -> {
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        });
        return dueNotifications.stream().map(this::map).toList();
    }

    public NotificationResponse createFromEvent(String type, NotificationRequest request) {
        return saveAndMap(type, request, NotificationStatus.SENT, LocalDateTime.now());
    }

    public NotificationResponse createNotification(CreateNotificationRequest request) {
        return saveAndMap(request.getType(), NotificationRequest.builder()
                .userId(request.getUserId())
                .appointmentId(request.getAppointmentId())
                .channel(request.getChannel())
                .message(request.getMessage())
                .scheduledFor(request.getScheduledFor())
                .build(), NotificationStatus.SENT, LocalDateTime.now());
    }

    private NotificationResponse saveAndMap(String type, NotificationRequest request, NotificationStatus status, LocalDateTime sentAt) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .appointmentId(request.getAppointmentId())
                .type(type)
                .channel(request.getChannel())
                .message(request.getMessage())
                .scheduledFor(request.getScheduledFor())
                .status(status)
                .sentAt(sentAt)
                .build();
        return map(notificationRepository.save(notification));
    }

    private NotificationResponse map(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .appointmentId(notification.getAppointmentId())
                .type(notification.getType())
                .channel(notification.getChannel())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .scheduledFor(notification.getScheduledFor())
                .sentAt(notification.getSentAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
