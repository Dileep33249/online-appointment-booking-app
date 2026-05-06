package com.app.notification.controller;

import com.app.notification.dto.CreateNotificationRequest;
import com.app.notification.dto.NotificationRequest;
import com.app.notification.dto.NotificationResponse;
import com.app.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/booking-confirmation")
    public ResponseEntity<NotificationResponse> createBookingConfirmation(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createBookingConfirmation(request));
    }

    @PostMapping("/cancellation")
    public ResponseEntity<NotificationResponse> createCancellation(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createCancellationNotification(request));
    }

    @PostMapping("/reminder")
    public ResponseEntity<NotificationResponse> scheduleReminder(@Valid @RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.scheduleReminder(request));
    }

    @PostMapping("/dispatch")
    public ResponseEntity<List<NotificationResponse>> dispatchDueReminders() {
        return ResponseEntity.ok(notificationService.dispatchDueReminders());
    }

    @PostMapping("/internal")
    public ResponseEntity<NotificationResponse> createInternalNotification(@Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createNotification(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(@PathVariable Long userId, Authentication authentication) {
        Long currentUserId = (Long) authentication.getPrincipal();
        if (!currentUserId.equals(userId)) {
            throw new RuntimeException("You can view only your own notifications");
        }
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }
}
