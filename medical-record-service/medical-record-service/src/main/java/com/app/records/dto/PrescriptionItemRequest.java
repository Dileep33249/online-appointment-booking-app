package com.app.records.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItemRequest {
    @NotBlank
    private String medicineName;
    private String dosage;
    private String frequency;
    private String duration;
}
