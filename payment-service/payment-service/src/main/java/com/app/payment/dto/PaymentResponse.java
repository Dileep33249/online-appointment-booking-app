package com.app.payment.dto;

import com.app.payment.entity.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long appointmentId;
    private Long userId;
    private Long providerId;
    private double amount;
    private PaymentStatus status;
    private String transactionId;
    private String paymentMethod;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
