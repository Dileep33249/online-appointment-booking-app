package com.app.auth.service;

import com.app.auth.dto.AuthResponseDto;
import com.app.auth.entity.User;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;



    @Mock
    private BCryptPasswordEncoder encoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AuthService authService;

    @Test
    void signup_shouldRegisterPatientSuccessfully() {
        User savedUser = User.builder()
                .id(1L)
                .fullname("Test User")
                .email("test@example.com")
                .password("encoded-password")
                .role("PATIENT")
                .provider("LOCAL")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("secret")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        String response = authService.signup("Test User", "test@example.com", "secret", null, null, null);

        assertEquals("User registered successfully", response);
        verify(restTemplate, never()).postForObject(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void signup_shouldCreateDoctorProfileWhenRoleIsDoctor() {
        User savedUser = User.builder()
                .id(2L)
                .fullname("Dr Test")
                .email("doctor@example.com")
                .password("encoded-password")
                .role("DOCTOR")
                .provider("LOCAL")
                .build();

        when(userRepository.findByEmail("doctor@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("secret")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtil.generateToken(2L, "doctor@example.com", "DOCTOR")).thenReturn("jwt-token");
        when(restTemplate.postForObject(eq("http://localhost:8082/providers/add"), any(HttpEntity.class), eq(String.class)))
                .thenReturn("ok");

        String response = authService.signup("Dr Test", "doctor@example.com", "secret", "doctor", "Cardiology", "City Clinic");

        assertEquals("User registered successfully", response);
        verify(restTemplate).postForObject(eq("http://localhost:8082/providers/add"), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void login_shouldReturnAuthResponseForValidCredentials() {
        User user = User.builder()
                .id(3L)
                .fullname("Login User")
                .email("login@example.com")
                .password("encoded-password")
                .role("PATIENT")
                .provider("LOCAL")
                .build();

        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("secret", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken(3L, "login@example.com", "PATIENT")).thenReturn("jwt-token");

        AuthResponseDto response = authService.login("login@example.com", "secret");

        assertEquals(3L, response.getUserId());
        assertEquals("jwt-token", response.getToken());
        assertEquals("Login successful", response.getMessage());
    }



}
