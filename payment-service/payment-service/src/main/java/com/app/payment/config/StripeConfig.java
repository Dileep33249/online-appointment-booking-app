package com.app.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @PostConstruct
    public void init() {
        secretKey = firstNonBlank(
                secretKey,
                System.getenv("STRIPE_SECRET_KEY"),
                readEnvFileValue("STRIPE_SECRET_KEY").orElse("")
        );
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        }
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
}
