package com.app.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloudinarySignatureResponseDto {
    private String cloudName;
    private String apiKey;
    private String folder;
    private long timestamp;
    private String signature;
}
