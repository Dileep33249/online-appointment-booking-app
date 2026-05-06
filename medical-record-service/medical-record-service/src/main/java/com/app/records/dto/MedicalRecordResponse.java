package com.app.records.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecordResponse {
    private Long id;
    private Long appointmentId;
    private Long patientId;
    private Long providerId;
    private String diagnosis;
    private String consultationNotes;
    private List<PrescriptionItemRequest> prescriptions;
    private List<AttachmentRequest> attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
