package com.app.payment.controller;

import com.app.payment.service.PaymentService;

import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class StripeWebhookController {

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        if ("payment_intent.succeeded".equals(event.getType())
                || "payment_intent.payment_failed".equals(event.getType())) {

            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (intent != null) {
                paymentService.updatePaymentStatusFromWebhook(
                        intent.getMetadata().get("appointmentId"),
                        intent.getStatus()
                );
            }
        }

        return ResponseEntity.ok("Webhook received");
    }
}