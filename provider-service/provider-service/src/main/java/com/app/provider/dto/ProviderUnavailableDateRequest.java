package com.app.provider.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderUnavailableDateRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String reason;
}
