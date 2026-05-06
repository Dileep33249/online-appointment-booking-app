package com.app.provider.controller;

import com.app.provider.dto.ProviderRequest;
import com.app.provider.dto.ProviderResponse;
import com.app.provider.service.ProviderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProviderController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProviderService service;

    @Test
    void add_shouldReturnCreatedProvider() throws Exception {
        ProviderRequest request = ProviderRequest.builder()
                .name("Dr House")
                .specialization("Diagnostics")
                .clinicName("Princeton")
                .imageUrl("https://example.com/doctor.jpg")
                .build();

        ProviderResponse response = ProviderResponse.builder()
                .id(1L)
                .userId(99L)
                .name("Dr House")
                .imageUrl("https://example.com/doctor.jpg")
                .specialization("Diagnostics")
                .clinicName("Princeton")
                .available(true)
                .verified(false)
                .createdAt(LocalDate.now())
                .build();

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(99L);
        when(service.addProvider(any(ProviderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/providers/add")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(99L))
                .andExpect(jsonPath("$.name").value("Dr House"));

        ArgumentCaptor<ProviderRequest> captor = ArgumentCaptor.forClass(ProviderRequest.class);
        verify(service).addProvider(captor.capture());
        assertEquals(99L, captor.getValue().getUserId());
    }

    @Test
    void getAll_shouldReturnProviders() throws Exception {
        ProviderResponse response = ProviderResponse.builder()
                .id(1L)
                .name("Dr House")
                .specialization("Diagnostics")
                .clinicName("Princeton")
                .build();

        when(service.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dr House"));
    }

    @Test
    void updateAvailability_shouldUseAuthenticatedUserId() throws Exception {
        ProviderResponse response = ProviderResponse.builder()
                .id(1L)
                .userId(44L)
                .name("Dr House")
                .available(false)
                .build();

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(44L);
        when(service.updateAvailability(44L, false)).thenReturn(response);

        mockMvc.perform(put("/providers/me/availability")
                        .principal(authentication)
                        .param("available", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        verify(service).updateAvailability(44L, false);
    }

    @Test
    void getBySpecialization_shouldDelegateToService() throws Exception {
        when(service.getBySpecialization(eq("Cardiology"))).thenReturn(List.of(
                ProviderResponse.builder().id(1L).name("Dr Heart").specialization("Cardiology").build()
        ));

        mockMvc.perform(get("/providers/specialization").param("name", "Cardiology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"));
    }
}
