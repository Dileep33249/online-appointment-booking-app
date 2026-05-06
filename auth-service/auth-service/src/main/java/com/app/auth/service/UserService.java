package com.app.auth.service;

import com.app.auth.dto.AdminUserDto;
import com.app.auth.dto.AdminUpdateDoctorRequestDto;
import com.app.auth.dto.CloudinarySignatureResponseDto;
import com.app.auth.dto.UserProfileDto;
import com.app.auth.entity.User;
import com.app.auth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final CloudinaryService cloudinaryService;

    public UserService(UserRepository userRepository,
                       RestTemplate restTemplate,
                       CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.cloudinaryService = cloudinaryService;
    }

    public UserProfileDto getUserProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setName(user.getFullname());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setImageUrl(user.getImageUrl());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    public UserProfileDto updateUserProfile(Long userId, UserProfileDto profileDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (profileDto.getImageUrl() == null || profileDto.getImageUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile image is required");
        }

        user.setFullname(profileDto.getName());
        user.setEmail(profileDto.getEmail());
        user.setImageUrl(profileDto.getImageUrl());

        userRepository.save(user);

        UserProfileDto response = new UserProfileDto();
        response.setId(user.getId());
        response.setName(user.getFullname());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setImageUrl(user.getImageUrl());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public CloudinarySignatureResponseDto createUploadSignature(String folder) {
        return cloudinaryService.createUploadSignature(folder);
    }

    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToAdminDto)
                .toList();
    }

    public List<AdminUserDto> getUsersByRole(String role) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() != null && user.getRole().equalsIgnoreCase(role))
                .map(this::mapToAdminDto)
                .toList();
    }

    public AdminUserDto updateDoctorByAdmin(Long userId, AdminUpdateDoctorRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"DOCTOR".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only doctor accounts can be updated here");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Another user already exists with email: " + normalizedEmail);
                });

        user.setFullname(ensureDoctorPrefix(request.getFullname()));
        user.setEmail(normalizedEmail);
        user.setImageUrl(request.getImageUrl().trim());

        return mapToAdminDto(userRepository.save(user));
    }

    @Transactional
    public void deleteUserById(Long userId, Long actingAdminUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin accounts can only be deleted directly in the database by the system administrator");
        }

        if ("DOCTOR".equalsIgnoreCase(user.getRole())) {
            try {
                restTemplate.delete("http://provider-service/providers/internal/by-user/" + userId);
            } catch (Exception ex) {
                throw new RuntimeException("Doctor profile deletion failed: " + ex.getMessage());
            }
        }

        userRepository.delete(user);
    }

    private AdminUserDto mapToAdminDto(User user) {
        return AdminUserDto.builder()
                .id(user.getId())
                .name(user.getFullname())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .imageUrl(user.getImageUrl())
                .build();
    }

    private String ensureDoctorPrefix(String fullname) {
        String normalizedName = fullname == null ? "" : fullname.trim();
        if (normalizedName.isEmpty()) {
            return normalizedName;
        }
        if (normalizedName.toLowerCase().startsWith("dr. ")) {
            return normalizedName;
        }
        if (normalizedName.toLowerCase().startsWith("dr ")) {
            return "Dr. " + normalizedName.substring(3).trim();
        }
        return "Dr. " + normalizedName;
    }
}
