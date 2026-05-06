package com.app.records.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentRequest {
    @NotBlank
    private String fileName;
    @NotBlank
    private String fileUrl;
    private String fileType;
}
