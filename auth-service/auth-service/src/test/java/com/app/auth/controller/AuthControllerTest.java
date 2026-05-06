package com.app.auth.controller;

import com.app.auth.dto.AuthRequestDto;
import com.app.auth.dto.AuthResponseDto;
import com.app.auth.dto.LoginRequestDto;
import com.app.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService service;

    @Test
    void signup_shouldReturnCreatedStatus() throws Exception {
        AuthRequestDto request = AuthRequestDto.builder()
                .fullname("Test User")
                .email("test@example.com")
                .password("secret")
                .role("PATIENT")
                .build();

        when(service.signup("Test User", "test@example.com", "secret", "PATIENT", null, null))
                .thenReturn("User registered successfully");

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(service).signup("Test User", "test@example.com", "secret", "PATIENT", null, null);
    }

    @Test
    void login_shouldReturnAuthResponse() throws Exception {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("test@example.com")
                .password("secret")
                .build();

        AuthResponseDto response = AuthResponseDto.builder()
                .userId(1L)
                .email("test@example.com")
                .role("PATIENT")
                .token("jwt-token")
                .message("Login successful")
                .build();

        when(service.login("test@example.com", "secret")).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }
}
