package com.app.payment.dto;

import com.app.payment.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripePaymentIntentResponse {
    private Long paymentId;
    private Long appointmentId;
    private String paymentIntentId;
    private String clientSecret;
    private String publishableKey;
    private String currency;
    private double amount;
    private PaymentStatus status;
}
