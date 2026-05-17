package com.app.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount must be zero or greater")
    private double amount;

    private String paymentMethod; // "DEMO" or actual payment provider

    private String description;

    private String mockOutcome; // SUCCESS, FAILED, or PENDING for demo payments
}
