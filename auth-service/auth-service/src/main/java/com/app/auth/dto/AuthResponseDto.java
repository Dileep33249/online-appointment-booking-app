package com.app.auth.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {
    private Long userId;
    private String email;
    private String role;
    private boolean mustChangePassword;
    private String token;
    private String message;
}
