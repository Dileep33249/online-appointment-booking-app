package com.app.auth.controller;

import com.app.auth.dto.AdminCreateDoctorRequestDto;
import com.app.auth.dto.AdminUpdateDoctorRequestDto;
import com.app.auth.dto.AdminUserDto;
import com.app.auth.dto.CloudinarySignatureResponseDto;
import com.app.auth.dto.UserProfileDto;
import com.app.auth.service.AuthService;
import com.app.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        UserProfileDto user = userService.getUserProfileById(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateCurrentUser(
            Authentication authentication,
            @RequestBody UserProfileDto profileDto) {
        Long userId = (Long) authentication.getPrincipal();
        UserProfileDto updated = userService.updateUserProfile(userId, profileDto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/internal/{userId}")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable Long userId) {
        UserProfileDto user = userService.getUserProfileById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/admin/patients")
    public ResponseEntity<List<AdminUserDto>> getPatients() {
        return ResponseEntity.ok(userService.getUsersByRole("PATIENT"));
    }

    @GetMapping("/admin/doctors")
    public ResponseEntity<List<AdminUserDto>> getDoctors() {
        return ResponseEntity.ok(userService.getUsersByRole("DOCTOR"));
    }

    @GetMapping("/upload-signature")
    public ResponseEntity<CloudinarySignatureResponseDto> getUploadSignature(@RequestParam(required = false) String folder) {
        return ResponseEntity.ok(userService.createUploadSignature(folder));
    }

    @PostMapping("/admin/doctors")
    public ResponseEntity<String> createDoctor(@Valid @RequestBody AdminCreateDoctorRequestDto request) {
        return ResponseEntity.ok(authService.createDoctorAccount(request));
    }

    @PutMapping("/admin/doctors/{userId}")
    public ResponseEntity<AdminUserDto> updateDoctorByAdmin(@PathVariable Long userId,
                                                            @Valid @RequestBody AdminUpdateDoctorRequestDto request) {
        return ResponseEntity.ok(userService.updateDoctorByAdmin(userId, request));
    }

    @DeleteMapping("/admin/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId, Authentication authentication) {
        Long adminUserId = (Long) authentication.getPrincipal();
        userService.deleteUserById(userId, adminUserId);
        return ResponseEntity.noContent().build();
    }
}
