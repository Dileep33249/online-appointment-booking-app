package com.app.provider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderUnavailableDateResponse {
    private Long id;
    private Long providerId;
    private LocalDate date;
    private String reason;
}
