package com.app.appointment.service;

import com.app.appointment.dto.AppointmentNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public RabbitMQProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendNotificationEvent(AppointmentNotificationEvent event) {
        log.info("Sending notification event to RabbitMQ: {}", event);
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
