package com.app.appointment.service;

import com.app.appointment.dto.AppointmentNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQProducer rabbitMQProducer;

    @BeforeEach
    void setUp() {
        // Set private fields populated by @Value in production
        ReflectionTestUtils.setField(rabbitMQProducer, "exchange", "notification-exchange");
        ReflectionTestUtils.setField(rabbitMQProducer, "routingKey", "notification-routing-key");
    }

    @Test
    void testSendNotificationEvent() {
        AppointmentNotificationEvent event = mock(AppointmentNotificationEvent.class);

        rabbitMQProducer.sendNotificationEvent(event);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("notification-exchange"),
                eq("notification-routing-key"),
                eq(event)
        );
    }
}
