package com.app.records.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMedicalRecordRequest {
    @NotNull
    private Long patientId;
    @NotNull
    private Long providerId;
    private String diagnosis;
    private String consultationNotes;
}
