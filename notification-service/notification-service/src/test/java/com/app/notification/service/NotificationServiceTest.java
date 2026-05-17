package com.app.notification.service;

import com.app.notification.dto.NotificationRequest;
import com.app.notification.dto.NotificationResponse;
import com.app.notification.entity.Notification;
import com.app.notification.entity.NotificationStatus;
import com.app.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationRequest request;
    private Notification notification;

    @BeforeEach
    void setUp() {
        request = NotificationRequest.builder()
                .userId(1L)
                .appointmentId(10L)
                .channel("EMAIL")
                .message("Your booking is confirmed.")
                .build();

        notification = Notification.builder()
                .id(100L)
                .userId(1L)
                .appointmentId(10L)
                .type("BOOKING_CONFIRMATION")
                .channel("EMAIL")
                .message("Your booking is confirmed.")
                .status(NotificationStatus.SENT)
                .build();
    }

    @Test
    void testCreateBookingConfirmation() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.createBookingConfirmation(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("BOOKING_CONFIRMATION", response.getType());
        assertEquals("Your booking is confirmed.", response.getMessage());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testCreateCancellationNotification() {
        Notification cancellationNotification = Notification.builder()
                .id(101L)
                .userId(1L)
                .appointmentId(10L)
                .type("CANCELLATION")
                .channel("EMAIL")
                .message("Your booking has been cancelled.")
                .status(NotificationStatus.SENT)
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(cancellationNotification);

        NotificationResponse response = notificationService.createCancellationNotification(request);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals("CANCELLATION", response.getType());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testGetUserNotifications() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getUserNotifications(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(100L, responses.get(0).getId());
        verify(notificationRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
    }
}
