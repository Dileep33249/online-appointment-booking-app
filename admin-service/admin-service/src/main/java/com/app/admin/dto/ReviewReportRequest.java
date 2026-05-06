package com.app.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReportRequest {
    @NotNull
    private Long providerId;
    @NotNull
    private Long reportedBy;
    @NotBlank
    private String reason;
}
