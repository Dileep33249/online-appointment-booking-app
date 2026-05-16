package com.app.appointment.service;

import com.app.appointment.dto.AppointmentNotificationEvent;
import com.app.appointment.dto.CreateNotificationRequest;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final RabbitMQProducer rabbitMQProducer;

    public NotificationService(RabbitMQProducer rabbitMQProducer) {
        this.rabbitMQProducer = rabbitMQProducer;
    }

    public void sendNotification(CreateNotificationRequest request) {
        // Fallback or keep for specific internal calls if needed, 
        // but let's route to RabbitMQ as well.
        publish(AppointmentNotificationEvent.builder()
                .userId(request.getUserId())
                .appointmentId(request.getAppointmentId())
                .type(request.getType())
                .channel(request.getChannel())
                .message(request.getMessage())
                .scheduledFor(request.getScheduledFor())
                .build());
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
        try {
            rabbitMQProducer.sendNotificationEvent(event);
        } catch (Exception e) {
            System.err.println("Failed to send notification via RabbitMQ: " + e.getMessage());
            // Don't throw the exception, let the transaction commit successfully
        }
    }
}
