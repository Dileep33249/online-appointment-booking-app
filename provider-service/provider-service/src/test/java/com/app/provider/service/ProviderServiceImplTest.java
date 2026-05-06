package com.app.provider.service;

import com.app.provider.dto.ProviderRequest;
import com.app.provider.dto.ProviderResponse;
import com.app.provider.entity.Provider;
import com.app.provider.repository.ProviderRepository;
import com.app.provider.repository.ProviderUnavailableDateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceImplTest {

    @Mock
    private ProviderRepository repo;

    @Mock
    private ProviderUnavailableDateRepository unavailableDateRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ProviderServiceImpl service;

    @Test
    void addProvider_shouldSaveNewProvider() {
        ProviderRequest request = ProviderRequest.builder()
                .userId(10L)
                .name("Dr Strange")
                .specialization("Cardiology")
                .imageUrl("https://example.com/doctor.jpg")
                .qualification("MD")
                .experienceYears(7)
                .bio("Experienced doctor")
                .clinicName("City Clinic")
                .clinicAddress("Delhi")
                .minimumFees(800)
                .build();

        Provider savedProvider = Provider.builder()
                .id(1L)
                .userId(10L)
                .name("Dr Strange")
                .specialization("Cardiology")
                .imageUrl("https://example.com/doctor.jpg")
                .qualification("MD")
                .experienceYears(7)
                .bio("Experienced doctor")
                .clinicName("City Clinic")
                .clinicAddress("Delhi")
                .avgRating(0.0)
                .minimumFees(800)
                .verified(false)
                .available(true)
                .createdAt(LocalDate.now())
                .build();

        when(repo.findByUserId(10L)).thenReturn(Optional.empty());
        when(repo.save(any(Provider.class))).thenReturn(savedProvider);

        ProviderResponse response = service.addProvider(request);

        assertEquals(1L, response.getId());
        assertEquals(10L, response.getUserId());
        assertEquals("Dr Strange", response.getName());
        assertFalse(response.isVerified());
        assertTrue(response.isAvailable());
        verify(repo).save(any(Provider.class));
    }

    @Test
    void addProvider_shouldThrowWhenProfileAlreadyExists() {
        ProviderRequest request = ProviderRequest.builder()
                .userId(10L)
                .name("Dr Strange")
                .specialization("Cardiology")
                .imageUrl("https://example.com/doctor.jpg")
                .clinicName("City Clinic")
                .build();

        when(repo.findByUserId(10L)).thenReturn(Optional.of(new Provider()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.addProvider(request));

        assertEquals("Provider profile already exists for this doctor", exception.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void getAll_shouldReturnOnlyVerifiedProviders() {
        Provider verifiedProvider = Provider.builder()
                .id(1L)
                .userId(11L)
                .name("Verified Doctor")
                .specialization("Dermatology")
                .imageUrl("https://example.com/verified.jpg")
                .clinicName("A Clinic")
                .avgRating(4.5)
                .minimumFees(500)
                .verified(true)
                .available(true)
                .createdAt(LocalDate.now())
                .build();

        Provider unverifiedProvider = Provider.builder()
                .id(2L)
                .userId(12L)
                .name("Pending Doctor")
                .specialization("ENT")
                .imageUrl("https://example.com/pending.jpg")
                .clinicName("B Clinic")
                .avgRating(4.0)
                .minimumFees(400)
                .verified(false)
                .available(true)
                .createdAt(LocalDate.now())
                .build();

        when(repo.findAll()).thenReturn(List.of(verifiedProvider, unverifiedProvider));

        List<ProviderResponse> responses = service.getAll();

        assertEquals(1, responses.size());
        assertEquals("Verified Doctor", responses.getFirst().getName());
    }

    @Test
    void updateAvailability_shouldUpdateProviderAvailability() {
        Provider provider = Provider.builder()
                .id(1L)
                .userId(21L)
                .name("Dr Who")
                .specialization("General")
                .imageUrl("https://example.com/general.jpg")
                .clinicName("Main Clinic")
                .avgRating(4.1)
                .minimumFees(300)
                .verified(true)
                .available(true)
                .createdAt(LocalDate.now())
                .build();

        when(repo.findByUserId(21L)).thenReturn(Optional.of(provider));
        when(repo.save(provider)).thenReturn(provider);

        ProviderResponse response = service.updateAvailability(21L, false);

        assertFalse(response.isAvailable());
        verify(repo).save(provider);
    }
}
