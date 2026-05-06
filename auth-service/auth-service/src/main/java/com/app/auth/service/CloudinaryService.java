package com.app.auth.service;

import com.app.auth.dto.CloudinarySignatureResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    public CloudinarySignatureResponseDto createUploadSignature(String requestedFolder) {
        loadFallbackConfigurationIfNeeded();
        validateConfiguration();

        String folder = sanitizeFolder(requestedFolder);
        long timestamp = System.currentTimeMillis() / 1000;
        String signatureBase = "folder=" + folder + "&timestamp=" + timestamp + apiSecret;

        return CloudinarySignatureResponseDto.builder()
                .cloudName(cloudName)
                .apiKey(apiKey)
                .folder(folder)
                .timestamp(timestamp)
                .signature(sha1Hex(signatureBase))
                .build();
    }

    private void loadFallbackConfigurationIfNeeded() {
        if (isConfigured()) {
            return;
        }

        cloudName = firstNonBlank(
                cloudName,
                System.getenv("CLOUDINARY_CLOUD_NAME"),
                readEnvFileValue("CLOUDINARY_CLOUD_NAME").orElse("")
        );
        apiKey = firstNonBlank(
                apiKey,
                System.getenv("CLOUDINARY_API_KEY"),
                readEnvFileValue("CLOUDINARY_API_KEY").orElse("")
        );
        apiSecret = firstNonBlank(
                apiSecret,
                System.getenv("CLOUDINARY_API_SECRET"),
                readEnvFileValue("CLOUDINARY_API_SECRET").orElse("")
        );
    }

    private boolean isConfigured() {
        return !cloudName.isBlank() && !apiKey.isBlank() && !apiSecret.isBlank();
    }

    private void validateConfiguration() {
        if (!isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cloudinary is not configured. Set cloudinary.cloud-name, cloudinary.api-key, and cloudinary.api-secret."
            );
        }
    }

    private String sanitizeFolder(String requestedFolder) {
        String fallback = "medibook/profiles";
        String folder = requestedFolder == null ? fallback : requestedFolder.trim();

        if (folder.isBlank()) {
            folder = fallback;
        }

        if (!folder.matches("[A-Za-z0-9_/-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Cloudinary folder name");
        }

        return folder;
    }

    private String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create Cloudinary signature");
        }
    }

    private Optional<String> readEnvFileValue(String key) {
        List<Path> candidates = List.of(
                Paths.get(".env"),
                Paths.get("auth-service", "auth-service", ".env"),
                Paths.get("online-appointment-app", "auth-service", "auth-service", ".env")
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
                // Try the next candidate path.
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
