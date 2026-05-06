package com.app.auth.service;

import com.app.auth.dto.AdminUserDto;
import com.app.auth.dto.UserProfileDto;
import com.app.auth.entity.User;
import com.app.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserProfileById_shouldReturnMappedProfile() {
        User user = User.builder()
                .id(1L)
                .fullname("Test User")
                .email("test@example.com")
                .password("secret")
                .role("PATIENT")
                .provider("LOCAL")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileDto response = userService.getUserProfileById(1L);

        assertEquals("Test User", response.getName());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void updateUserProfile_shouldPersistUpdatedValues() {
        User user = User.builder()
                .id(2L)
                .fullname("Old Name")
                .email("old@example.com")
                .password("secret")
                .role("PATIENT")
                .provider("LOCAL")
                .build();

        UserProfileDto request = new UserProfileDto("New Name", "new@example.com");
        request.setImageUrl("https://example.com/patient.jpg");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        UserProfileDto response = userService.updateUserProfile(2L, request);

        assertEquals("New Name", response.getName());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("https://example.com/patient.jpg", response.getImageUrl());
        verify(userRepository).save(user);
    }

    @Test
    void getUsersByRole_shouldFilterUsersCaseInsensitively() {
        User patient = User.builder()
                .id(3L)
                .fullname("Patient One")
                .email("patient@example.com")
                .password("secret")
                .role("PATIENT")
                .provider("LOCAL")
                .build();

        User admin = User.builder()
                .id(4L)
                .fullname("Admin One")
                .email("admin@example.com")
                .password("secret")
                .role("ADMIN")
                .provider("LOCAL")
                .build();

        when(userRepository.findAll()).thenReturn(List.of(patient, admin));

        List<AdminUserDto> response = userService.getUsersByRole("patient");

        assertEquals(1, response.size());
        assertEquals("Patient One", response.getFirst().getName());
    }
}
