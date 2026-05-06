package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderVerificationRequest {
    @NotNull
    private Long providerId;
    @NotBlank
    private String providerName;
    private String remarks;
}
