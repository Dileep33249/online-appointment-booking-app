package com.app.auth.controller;

import com.app.auth.dto.AdminUserDto;
import com.app.auth.dto.UserProfileDto;
import com.app.auth.service.AuthService;
import com.app.auth.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @Test
    void getCurrentUser_shouldReturnLoggedInUserProfile() throws Exception {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(7L);
        when(userService.getUserProfileById(7L)).thenReturn(new UserProfileDto("Current User", "current@example.com"));

        mockMvc.perform(get("/users/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Current User"));
    }

    @Test
    void updateCurrentUser_shouldUseAuthenticatedUserId() throws Exception {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(8L);

        UserProfileDto request = new UserProfileDto("Updated User", "updated@example.com");
        request.setImageUrl("https://example.com/profile.jpg");
        when(userService.updateUserProfile(8L, request)).thenReturn(request);

        mockMvc.perform(put("/users/me")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        verify(userService).updateUserProfile(8L, request);
    }

    @Test
    void getPatients_shouldReturnFilteredUsers() throws Exception {
        when(userService.getUsersByRole("PATIENT")).thenReturn(List.of(
                AdminUserDto.builder().id(1L).name("Patient User").email("p@example.com").role("PATIENT").build()
        ));

        mockMvc.perform(get("/users/admin/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("PATIENT"));
    }
}
