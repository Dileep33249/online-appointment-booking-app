package com.app.auth.service;

import com.app.auth.dto.AdminCreateDoctorRequestDto;
import com.app.auth.dto.AuthResponseDto;
import com.app.auth.entity.User;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtUtil;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder encoder,
                       JwtUtil jwtUtil,
                       NotificationService notificationService,
                       RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.notificationService = notificationService;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public String signup(String fullname,
                         String email,
                         String password,
                         String role,
                         String specialization,
                         String clinicName) {
        String userRole = (role == null || role.isBlank()) ? "PATIENT" : role.trim().toUpperCase();

        if (!userRole.equals("PATIENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only patients can register publicly");
        }

        createLocalUser(fullname, email, password, userRole, false);

        return "User registered successfully";
    }

    @Transactional
    public String createDoctorAccount(AdminCreateDoctorRequestDto request) {
        String displayName = ensureDoctorPrefix(request.getFullname());

        User doctor = createLocalUser(
                displayName,
                request.getEmail(),
                request.getPassword(),
                "DOCTOR",
                true,
                request.getImageUrl()
        );

        createDoctorProfile(
                doctor,
                request.getSpecialization(),
                request.getClinicName(),
                request.getQualification(),
                request.getExperienceYears(),
                request.getBio(),
                request.getClinicAddress(),
                request.getMinimumFees(),
                request.getImageUrl()
        );

        return "Doctor account created successfully";
    }

    public AuthResponseDto login(String email, String password) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        String normalizedPassword = password == null ? null : password.trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!encoder.matches(normalizedPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .mustChangePassword(user.isMustChangePassword())
                .token(token)
                .message("Login successful")
                .build();
    }

    @Transactional
    public String changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        user.setPassword(encoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        return "Password changed successfully";
    }

    private User createLocalUser(String fullname,
                                 String email,
                                 String password,
                                 String role,
                                 boolean mustChangePassword,
                                 String imageUrl) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        String normalizedFullname = fullname == null ? null : fullname.trim();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new RuntimeException("User already exists with email: " + normalizedEmail);
        }

        User user = User.builder()
                .fullname(normalizedFullname)
                .email(normalizedEmail)
                .password(encoder.encode(password))
                .role(role)
                .provider("LOCAL")
                .imageUrl(imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim())
                .mustChangePassword(mustChangePassword)
                .build();

        return userRepository.save(user);
    }

    private User createLocalUser(String fullname, String email, String password, String role, boolean mustChangePassword) {
        return createLocalUser(fullname, email, password, role, mustChangePassword, null);
    }

    private void createDoctorProfile(User user,
                                     String specialization,
                                     String clinicName,
                                     String qualification,
                                     Integer experienceYears,
                                     String bio,
                                     String clinicAddress,
                                     Double minimumFees,
                                     String imageUrl) {
        try {
            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", user.getId());
            requestBody.put("name", user.getFullname());
            requestBody.put("specialization", specialization == null || specialization.isBlank() ? "General" : specialization);
            requestBody.put("qualification", qualification == null ? "" : qualification);
            requestBody.put("experienceYears", experienceYears == null ? 0 : experienceYears);
            requestBody.put("bio", bio == null ? "" : bio);
            requestBody.put("clinicName", clinicName == null || clinicName.isBlank() ? "Default Clinic" : clinicName);
            requestBody.put("clinicAddress", clinicAddress == null ? "" : clinicAddress);
            requestBody.put("minimumFees", minimumFees == null ? 0.0 : minimumFees);
            requestBody.put("imageUrl", imageUrl == null ? "" : imageUrl.trim());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            restTemplate.postForObject("http://provider-service/providers/add", request, String.class);
        } catch (Exception ex) {
            throw new RuntimeException("Doctor profile creation failed: " + ex.getMessage());
        }
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
