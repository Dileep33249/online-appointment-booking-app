package com.app.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String imageUrl;
    private LocalDateTime createdAt;

    public UserProfileDto(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
