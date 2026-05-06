package com.app.appointment.service;

import com.app.appointment.dto.AppointmentNotificationEvent;
import com.app.appointment.dto.CreateNotificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {
    private final RestTemplate restTemplate;

    public NotificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendNotification(CreateNotificationRequest request) {
        restTemplate.postForObject(
                "http://notification-service/notifications/internal",
                request,
                Object.class
        );
    }

    public void sendBookingConfirmation(AppointmentNotificationEvent event) {
        publish(event);
    }

    public void sendCancellationNotification(AppointmentNotificationEvent event) {
        publish(event);
    }

    public void sendRescheduleNotification(AppointmentNotificationEvent event) {
        publish(event);
    }

    public void sendCompletionNotification(AppointmentNotificationEvent event) {
        publish(event);
    }

    private void publish(AppointmentNotificationEvent event) {
        sendNotification(CreateNotificationRequest.builder()
                .userId(event.getUserId())
                .appointmentId(event.getAppointmentId())
                .type(event.getType())
                .channel(event.getChannel())
                .message(event.getMessage())
                .scheduledFor(event.getScheduledFor())
                .build());
    }
}
