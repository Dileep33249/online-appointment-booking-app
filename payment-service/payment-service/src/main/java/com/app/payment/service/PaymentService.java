package com.app.payment.service;

import com.app.payment.dto.PaymentRequest;
import com.app.payment.dto.PaymentResponse;
import com.app.payment.dto.StripeConfigResponse;
import com.app.payment.dto.StripePaymentConfirmRequest;
import com.app.payment.dto.StripePaymentIntentResponse;
import com.app.payment.entity.Payment;
import com.app.payment.entity.PaymentStatus;
import com.app.payment.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentRetrieveParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.stripe.Stripe;
import com.stripe.net.RequestOptions;
@Service
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;
    private final String stripePublishableKey;
    private final String stripeCurrency;
    private final boolean stripeConfigured;


    public PaymentService(PaymentRepository paymentRepository,
                          RestTemplate restTemplate,
                          @Value("${stripe.publishable-key:}") String stripePublishableKey,
                          @Value("${stripe.currency:inr}") String stripeCurrency,
                          @Value("${stripe.secret-key:}") String stripeSecretKey) {
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
        this.stripePublishableKey = firstNonBlank(
                stripePublishableKey,
                System.getenv("STRIPE_PUBLISHABLE_KEY"),
                readEnvFileValue("STRIPE_PUBLISHABLE_KEY").orElse("")
        );
        String resolvedSecretKey = firstNonBlank(
                stripeSecretKey,
                System.getenv("STRIPE_SECRET_KEY"),
                readEnvFileValue("STRIPE_SECRET_KEY").orElse("")
        );
        String resolvedCurrency = firstNonBlank(
                stripeCurrency,
                System.getenv("STRIPE_CURRENCY"),
                readEnvFileValue("STRIPE_CURRENCY").orElse("inr")
        ).trim().toLowerCase(Locale.ROOT);
        this.stripeCurrency = resolvedCurrency.isBlank() ? "inr" : resolvedCurrency;
        this.stripeConfigured = !resolvedSecretKey.isBlank() && !this.stripePublishableKey.isBlank();
        Stripe.apiKey = resolvedSecretKey;
    }

    private Optional<String> readEnvFileValue(String key) {
        List<Path> candidates = List.of(
                Paths.get(".env"),
                Paths.get("payment-service", "payment-service", ".env"),
                Paths.get("online-appointment-app", "payment-service", "payment-service", ".env")
        );

        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }

            try {
                Optional<String> value = Files.readAllLines(candidate).stream()
                        .map(String::trim)
                        .filter(line -> !line.isBlank() && !line.startsWith("#"))
                        .filter(line -> line.startsWith(key + "="))
                        .map(line -> line.substring((key + "=").length()).trim())
                        .map(this::stripQuotes)
                        .findFirst();

                if (value.isPresent() && !value.get().isBlank()) {
                    return value;
                }
            } catch (IOException ignored) {
                // Try next path.
                log.error("exception occured", ignored.getMessage());
            }
        }

        return Optional.empty();
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /**
     * Process a demo payment for appointment
     * In production, this would integrate with Stripe, PayPal, etc.
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for appointment: {}", request.getAppointmentId());

        var existingPayment = paymentRepository.findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(request.getAppointmentId());
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.SUCCESS) {
            log.warn("Successful payment already exists for appointment: {}", request.getAppointmentId());
            throw new RuntimeException("Payment already completed for this appointment");
        }

        Payment payment = existingPayment.orElseGet(Payment::new);
        payment.setAppointmentId(request.getAppointmentId());
        payment.setUserId(request.getUserId());
        payment.setProviderId(request.getProviderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "DEMO");
        payment.setDescription(request.getDescription());
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setTransactionId("MOCK-" + UUID.randomUUID().toString());

        PaymentStatus finalStatus = resolveMockOutcome(request.getMockOutcome());
        payment.setStatus(finalStatus);
        log.info("Demo payment {} for appointment {} with txn {}", finalStatus, request.getAppointmentId(), payment.getTransactionId());

        Payment savedPayment = paymentRepository.save(payment);
        return mapToResponse(savedPayment);
    }

    public StripeConfigResponse getStripeConfig() {
        if (!stripeConfigured) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stripe is not configured. Set stripe.publishable-key and stripe.secret-key.");
        }
        return StripeConfigResponse.builder()
                .publishableKey(stripePublishableKey)
                .currency(stripeCurrency)
                .build();
    }

    public StripePaymentIntentResponse createStripePaymentIntent(PaymentRequest request) {
        ensureStripeConfigured();
        log.info("Creating Stripe PaymentIntent for appointment: {}", request.getAppointmentId());

        var existingPayment = paymentRepository.findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(request.getAppointmentId());
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Payment already completed for this appointment");
        }

        Payment payment = existingPayment.orElseGet(Payment::new);
        cancelExistingStripeIntentIfPossible(payment);

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toSmallestUnit(request.getAmount()))
                    .setCurrency(stripeCurrency)
                    .setDescription(request.getDescription() != null && !request.getDescription().isBlank()
                            ? request.getDescription()
                            : "Appointment payment #" + request.getAppointmentId())
                    .putMetadata("appointmentId", String.valueOf(request.getAppointmentId()))
                    .putMetadata("userId", String.valueOf(request.getUserId()))
                    .putMetadata("providerId", String.valueOf(request.getProviderId()))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build()
                    )
                    .build();

            // Use a more unique key to allow retries if parameters changed (e.g., description or metadata)
            String idempotencyKey = "pay_" + request.getAppointmentId() + "_" + UUID.randomUUID().toString().substring(0, 8);

            PaymentIntent paymentIntent = PaymentIntent.create(
                    params,
                    RequestOptions.builder()
                            .setIdempotencyKey(idempotencyKey)
                            .build()
            );

            payment.setAppointmentId(request.getAppointmentId());
            payment.setUserId(request.getUserId());
            payment.setProviderId(request.getProviderId());
            payment.setAmount(request.getAmount());
            payment.setPaymentMethod("STRIPE");
            payment.setDescription(params.getDescription());
            payment.setTransactionId(paymentIntent.getId());
            payment.setStatus(mapStripeStatus(paymentIntent.getStatus()));

            Payment savedPayment = paymentRepository.save(payment);
            return StripePaymentIntentResponse.builder()
                    .paymentId(savedPayment.getId())
                    .appointmentId(savedPayment.getAppointmentId())
                    .paymentIntentId(paymentIntent.getId())
                    .clientSecret(paymentIntent.getClientSecret())
                    .publishableKey(stripePublishableKey)
                    .currency(stripeCurrency)
                    .amount(savedPayment.getAmount())
                    .status(savedPayment.getStatus())
                    .build();
        } catch (StripeException exception) {
            log.error("Stripe PaymentIntent creation failed for appointment {}", request.getAppointmentId(), exception);
            throw new RuntimeException("Stripe payment initialization failed: " + exception.getMessage());
        }
    }

    public PaymentResponse confirmStripePayment(StripePaymentConfirmRequest request) {
        ensureStripeConfigured();

        Payment payment = paymentRepository.findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(request.getAppointmentId())
                .orElseThrow(() -> new RuntimeException("Payment not found for appointment: " + request.getAppointmentId()));

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(
                    request.getPaymentIntentId(),
                    PaymentIntentRetrieveParams.builder().build(),
                    null
            );

            String appointmentIdFromStripe = paymentIntent.getMetadata() != null
                    ? paymentIntent.getMetadata().get("appointmentId")
                    : null;
            if (appointmentIdFromStripe == null
                    || !String.valueOf(request.getAppointmentId()).equals(appointmentIdFromStripe)) {
                throw new RuntimeException("Payment intent does not belong to this appointment");
            }

            payment.setTransactionId(paymentIntent.getId());
            PaymentStatus newStatus = mapStripeStatus(paymentIntent.getStatus());
            payment.setStatus(newStatus);
            payment.setPaymentMethod(resolveStripeMethod(paymentIntent));
            Payment savedPayment = paymentRepository.save(payment);
            
            if (newStatus == PaymentStatus.SUCCESS) {
                try {
                    restTemplate.postForObject("http://appointment-service/appointments/" + request.getAppointmentId() + "/internal/confirm-payment", null, Object.class);
                    log.info("Notified appointment-service about successful payment (from confirmStripePayment) for appointment: {}", request.getAppointmentId());
                } catch (Exception e) {
                    log.error("Failed to notify appointment-service about payment success for appointment: {}", request.getAppointmentId(), e);
                }
            }
            
            return mapToResponse(savedPayment);
        } catch (StripeException exception) {
            log.error("Stripe PaymentIntent confirmation sync failed for {}", request.getPaymentIntentId(), exception);
            throw new RuntimeException("Unable to verify Stripe payment: " + exception.getMessage());
        }
    }

    /**
     * Get payment by appointment ID
     */
    public PaymentResponse getPaymentByAppointmentId(Long appointmentId) {
        var payment = paymentRepository.findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(appointmentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for appointment: " + appointmentId));
        return mapToResponse(payment);
    }

    /**
     * Get all payments for a user
     */
    public List<PaymentResponse> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments for a provider
     */
    public List<PaymentResponse> getProviderPayments(Long providerId) {
        return paymentRepository.findByProviderId(providerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get payment details
     */
    public PaymentResponse getPaymentDetails(Long paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        return mapToResponse(payment);
    }

    /**
     * Refund a payment
     */
    public PaymentResponse refundPayment(Long paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RuntimeException("Can only refund successful payments");
        }

        if ("STRIPE".equalsIgnoreCase(payment.getPaymentMethod()) && payment.getTransactionId() != null && !payment.getTransactionId().isBlank()) {
            ensureStripeConfigured();
            try {
                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(payment.getTransactionId())
                        .build();
                Refund.create(params);
            } catch (StripeException exception) {
                log.error("Stripe refund failed for payment {}", paymentId, exception);
                throw new RuntimeException("Stripe refund failed: " + exception.getMessage());
            }
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment refundedPayment = paymentRepository.save(payment);
        log.info("Payment refunded: {}", refundedPayment.getId());
        return mapToResponse(refundedPayment);
    }

    /**
     * Check if payment is successful
     */
    public boolean isPaymentSuccessful(Long appointmentId) {
        var payment = paymentRepository.findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(appointmentId);
        return payment.isPresent() && payment.get().getStatus() == PaymentStatus.SUCCESS;
    }

    private PaymentStatus resolveMockOutcome(String mockOutcome) {
        if (mockOutcome == null || mockOutcome.isBlank()) {
            return PaymentStatus.SUCCESS;
        }

        return switch (mockOutcome.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING", "PROCESSING" -> PaymentStatus.PENDING;
            case "FAILED", "FAILURE" -> PaymentStatus.FAILED;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            default -> PaymentStatus.SUCCESS;
        };
    }

    private void ensureStripeConfigured() {
        if (!stripeConfigured) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stripe is not configured. Set stripe.publishable-key and stripe.secret-key.");
        }
    }

    private long toSmallestUnit(double amount) {
        return BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();
    }

    private void cancelExistingStripeIntentIfPossible(Payment payment) {
        if (payment.getTransactionId() == null || payment.getTransactionId().isBlank()) {
            return;
        }

        if (!"STRIPE".equalsIgnoreCase(payment.getPaymentMethod())) {
            return;
        }

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(payment.getTransactionId());
            if (paymentIntent != null) {
                String status = paymentIntent.getStatus();
                if ("requires_payment_method".equals(status)
                        || "requires_confirmation".equals(status)
                        || "requires_action".equals(status)
                        || "processing".equals(status)) {
                    paymentIntent.cancel(PaymentIntentCancelParams.builder().build());
                }
            }
        } catch (StripeException exception) {
            log.warn("Could not cancel previous Stripe PaymentIntent {}", payment.getTransactionId(), exception);
        }
    }

    private PaymentStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return PaymentStatus.PENDING;
        }

        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.SUCCESS;
            case "processing" -> PaymentStatus.PROCESSING;
            case "canceled" -> PaymentStatus.CANCELLED;
            case "requires_payment_method" -> PaymentStatus.FAILED;
            case "requires_capture", "requires_confirmation", "requires_action" -> PaymentStatus.PENDING;
            default -> PaymentStatus.PENDING;
        };
    }

    private String resolveStripeMethod(PaymentIntent paymentIntent) {
        if (paymentIntent.getPaymentMethodTypes() != null && !paymentIntent.getPaymentMethodTypes().isEmpty()) {
            return "STRIPE_" + paymentIntent.getPaymentMethodTypes().get(0).toUpperCase(Locale.ROOT);
        }
        return "STRIPE";
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .appointmentId(payment.getAppointmentId())
                .userId(payment.getUserId())
                .providerId(payment.getProviderId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paymentMethod(payment.getPaymentMethod())
                .description(payment.getDescription())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
    public void updatePaymentStatusFromWebhook(String appointmentId, String stripeStatus) {
        paymentRepository.findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(Long.valueOf(appointmentId))
                .ifPresent(payment -> {
                    payment.setStatus(mapStripeStatus(stripeStatus));
                    paymentRepository.save(payment);
                    
                    if ("succeeded".equals(stripeStatus)) {
                        try {
                            restTemplate.postForObject("http://appointment-service/appointments/" + appointmentId + "/internal/confirm-payment", null, Object.class);
                            log.info("Notified appointment-service about successful payment for appointment: {}", appointmentId);
                        } catch (Exception e) {
                            log.error("Failed to notify appointment-service about payment success for appointment: {}", appointmentId, e);
                        }
                    }
                });
    }
}
