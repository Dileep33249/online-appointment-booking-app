package com.app.consultation.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationCompletionRequest {
    private String diagnosis;
    private String notes;
}
