package com.app.notification.service;

import com.app.notification.dto.AppointmentNotificationEvent;
import com.app.notification.dto.CreateNotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RabbitMQConsumer {

    private final NotificationService notificationService;

    public RabbitMQConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void consume(AppointmentNotificationEvent event) {
        log.info("Received notification event from RabbitMQ: {}", event);
        
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(event.getUserId());
        request.setMessage(event.getMessage());
        request.setType(event.getType());
        request.setChannel(event.getChannel());
        
        notificationService.createNotification(request);
    }
}
