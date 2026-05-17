package com.app.payment.service;

import com.app.payment.dto.PaymentRequest;
import com.app.payment.dto.PaymentResponse;
import com.app.payment.entity.Payment;
import com.app.payment.entity.PaymentStatus;
import com.app.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RestTemplate restTemplate;

    private PaymentService paymentService;
    private Payment mockPayment;

    @BeforeEach
    void setUp() {
        // Initialize PaymentService manually using the constructor to handle String fields
        paymentService = new PaymentService(
                paymentRepository,
                restTemplate,
                "test-pub-key",
                "inr",
                "test-secret-key"
        );

        mockPayment = Payment.builder()
                .id(1L)
                .appointmentId(10L)
                .userId(100L)
                .providerId(200L)
                .amount(500.0)
                .paymentMethod("DEMO")
                .description("Consultation Fee")
                .status(PaymentStatus.SUCCESS)
                .transactionId("MOCK-TXN-123")
                .build();
    }

    @Test
    void testProcessPaymentSuccess() {
        PaymentRequest request = new PaymentRequest();
        request.setAppointmentId(10L);
        request.setUserId(100L);
        request.setProviderId(200L);
        request.setAmount(500.0);
        request.setPaymentMethod("DEMO");
        request.setDescription("Consultation Fee");
        request.setMockOutcome("SUCCESS");

        when(paymentRepository.findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(10L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("MOCK-TXN-123", response.getTransactionId());
        verify(paymentRepository, times(1)).findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(10L);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void testProcessPaymentAlreadyCompleted() {
        PaymentRequest request = new PaymentRequest();
        request.setAppointmentId(10L);

        when(paymentRepository.findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(10L)).thenReturn(Optional.of(mockPayment));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> paymentService.processPayment(request));
        assertEquals("Payment already completed for this appointment", exception.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void testIsPaymentSuccessful() {
        when(paymentRepository.findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(10L)).thenReturn(Optional.of(mockPayment));

        boolean isSuccessful = paymentService.isPaymentSuccessful(10L);

        assertTrue(isSuccessful);
        verify(paymentRepository, times(1)).findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(10L);
    }
}
