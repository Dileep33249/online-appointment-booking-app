package com.app.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionSummaryRequest {
    @NotBlank
    private String prescriptionSummary;
}
