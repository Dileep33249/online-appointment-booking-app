package com.app.provider.service;

import com.app.provider.dto.ProviderRequest;
import com.app.provider.dto.ProviderResponse;
import com.app.provider.dto.ProviderImageUpdateRequest;
import com.app.provider.dto.ProviderUnavailableDateRequest;
import com.app.provider.dto.ProviderUnavailableDateResponse;
import com.app.provider.entity.Provider;
import com.app.provider.entity.ProviderUnavailableDate;
import com.app.provider.repository.ProviderRepository;
import com.app.provider.repository.ProviderUnavailableDateRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProviderServiceImpl implements ProviderService {

    private final ProviderRepository repo;
    private final ProviderUnavailableDateRepository unavailableDateRepository;
    private final RestTemplate restTemplate;

    public ProviderServiceImpl(ProviderRepository repo,
                               ProviderUnavailableDateRepository unavailableDateRepository,
                               RestTemplate restTemplate) {
        this.repo = repo;
        this.unavailableDateRepository = unavailableDateRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "providersById", allEntries = true),
            @CacheEvict(value = "providersByUserId", allEntries = true),
            @CacheEvict(value = "providersAll", allEntries = true),
            @CacheEvict(value = "providersAdminAll", allEntries = true),
            @CacheEvict(value = "providersSearch", allEntries = true),
            @CacheEvict(value = "providersBySpecialization", allEntries = true)
    })
    public ProviderResponse addProvider(ProviderRequest request) {
        if (repo.findByUserId(request.getUserId()).isPresent()) {
            throw new RuntimeException("Provider profile already exists for this doctor");
        }

        Provider provider = Provider.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .specialization(request.getSpecialization())
                .imageUrl(request.getImageUrl())
                .qualification(request.getQualification())
                .experienceYears(request.getExperienceYears())
                .bio(request.getBio())
                .clinicName(request.getClinicName())
                .clinicAddress(request.getClinicAddress())
                .avgRating(0.0)
                .minimumFees(request.getMinimumFees())
                .verified(true)
                .available(true)
                .build();

        return mapToResponse(repo.save(provider));
    }

    @Override
    @Cacheable(value = "providersById", key = "#id")
    public ProviderResponse getById(Long id) {
        return mapToResponse(repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found")));
    }

    @Override
    @Cacheable(value = "providersByUserId", key = "#userId")
    public ProviderResponse getByUserId(Long userId) {
        return mapToResponse(repo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Provider not found for this doctor")));
    }

    @Override
    @Cacheable("providersAll")
    public List<ProviderResponse> getAll() {
        return repo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable("providersAdminAll")
    public List<ProviderResponse> getAllAdmin() {
        return repo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "providersSearch", key = "#keyword == null ? '' : #keyword.toLowerCase()")
    public List<ProviderResponse> search(String keyword) {
        return repo.searchByNameOrSpecialization(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "providersBySpecialization", key = "#specialization == null ? '' : #specialization.toLowerCase()")
    public List<ProviderResponse> getBySpecialization(String specialization) {
        return repo.findBySpecialization(specialization).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "providersById", key = "#id"),
            @CacheEvict(value = "providersByUserId", allEntries = true),
            @CacheEvict(value = "providersAll", allEntries = true),
            @CacheEvict(value = "providersAdminAll", allEntries = true),
            @CacheEvict(value = "providersSearch", allEntries = true),
            @CacheEvict(value = "providersBySpecialization", allEntries = true)
    })
    public ProviderResponse verifyProvider(Long id) {
        Provider provider = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        provider.setVerified(true);
        return mapToResponse(repo.save(provider));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "providersById", key = "#id"),
            @CacheEvict(value = "providersByUserId", allEntries = true),
            @CacheEvict(value = "providersAll", allEntries = true),
            @CacheEvict(value = "providersAdminAll", allEntries = true),
            @CacheEvict(value = "providersSearch", allEntries = true),
            @CacheEvict(value = "providersBySpecialization", allEntries = true)
    })
    public ProviderResponse unverifyProvider(Long id) {
        Provider provider = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        provider.setVerified(false);
        return mapToResponse(repo.save(provider));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "providersById", allEntries = true),
            @CacheEvict(value = "providersByUserId", key = "#userId"),
            @CacheEvict(value = "providersAll", allEntries = true),
            @CacheEvict(value = "providersAdminAll", allEntries = true),
            @CacheEvict(value = "providersSearch", allEntries = true),
            @CacheEvict(value = "providersBySpecialization", allEntries = true)
    })
    public ProviderResponse updateMyProfile(Long userId, ProviderRequest request) {
        Provider provider = repo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Provider not found for this doctor"));

        provider.setName(request.getName());
        provider.setSpecialization(request.getSpecialization());
        provider.setImageUrl(request.getImageUrl());
        provider.setQualification(request.getQualification());
        provider.setExperienceYears(request.getExperienceYears());
        provider.setBio(request.getBio());
        provider.setClinicName(request.getClinicName());
        provider.setClinicAddress(request.getClinicAddress());
        provider.setMinimumFees(request.getMinimumFees());

        return mapToResponse(repo.save(provider));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "providersById", key = "#providerId"),
            @CacheEvict(value = "providersByUserId", allEntries = true),
            @CacheEvict(value = "providersAll", allEntries = true),
            @CacheEvict(value = "providersAdminAll", allEntries = true),
            @CacheEvict(value = "providersSearch", allEntries = true),
            @CacheEvict(value = "providersBySpecialization", allEntries = true)
    })
    public ProviderResponse updateProviderProfileByAdmin(Long providerId, ProviderRequest request) {
        Provider provider = repo.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        provider.setName(request.getName());
        provider.setSpecialization(request.getSpecialization());
        provider.setImageUrl(request.getImageUrl());
        provider.setQualification(request.getQualification());
        provider.setExperienceYears(request.getExperienceYears());
        provider.setBio(request.getBio());
        provider.setClinicName(request.getClinicName());
        provider.setClinicAddress(request.getClinicAddress());
        provider.setMinimumFees(request.getMinimumFees());

        return mapToResponse(repo.save(provider));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "providersById", key = "#providerId"),
            @CacheEvict(value = "providersByUserId", allEntries = true),
            @CacheEvict(value = "providersAll", allEntries = true),
            @CacheEvict(value = "providersAdminAll", allEntries = true),
            @CacheEvict(value = "providersSearch", allEntries = true),
            @CacheEvict(value = "providersBySpecialization", allEntries = true)
    })
    public ProviderResponse updateProviderImageByAdmin(Long providerId, ProviderImageUpdateRequest request) {
        Provider provider = repo.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        provider.setImageUrl(request.getImageUrl());
        return mapToResponse(repo.save(provider));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "providersById", allEntries = true),
            @CacheEvict(value = "providersByUserId", key = "#userId"),
            @CacheEvict(value = "providersAll", allEntries = true),
            @CacheEvict(value = "providersAdminAll", allEntries = true),
            @CacheEvict(value = "providersSearch", allEntries = true),
            @CacheEvict(value = "providersBySpecialization", allEntries = true)
    })
    public ProviderResponse updateAvailability(Long userId, boolean available) {
        Provider provider = repo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Provider not found for this doctor"));

        provider.setAvailable(available);
        return mapToResponse(repo.save(provider));
    }

    @Override
    public boolean isBookable(Long providerId) {
        Provider provider = repo.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));
        return provider.isAvailable();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "providersById", allEntries = true),
            @CacheEvict(value = "providersByUserId", key = "#userId"),
            @CacheEvict(value = "providersAll", allEntries = true),
            @CacheEvict(value = "providersAdminAll", allEntries = true),
            @CacheEvict(value = "providersSearch", allEntries = true),
            @CacheEvict(value = "providersBySpecialization", allEntries = true)
    })
    public void deleteByUserId(Long userId) {
        repo.findByUserId(userId).ifPresent(provider -> {
            unavailableDateRepository.deleteByProviderId(provider.getId());
            repo.delete(provider);
        });
    }

    @Override
    public ProviderUnavailableDateResponse markUnavailableDate(Long providerId, ProviderUnavailableDateRequest request) {
        Provider provider = repo.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(13);
        if (request.getDate().isBefore(today) || request.getDate().isAfter(maxDate)) {
            throw new RuntimeException("Unavailable dates can only be managed within the next two weeks");
        }

        if (unavailableDateRepository.existsByProviderIdAndDate(providerId, request.getDate())) {
            throw new RuntimeException("This doctor is already marked unavailable on that date");
        }

        ProviderUnavailableDate blockedDate = unavailableDateRepository.save(ProviderUnavailableDate.builder()
                .providerId(provider.getId())
                .date(request.getDate())
                .reason(request.getReason())
                .build());

        return mapUnavailableDate(blockedDate);
    }

    @Override
    @Transactional
    public void removeUnavailableDate(Long providerId, LocalDate date) {
        ProviderUnavailableDate blockedDate = unavailableDateRepository.findByProviderIdAndDate(providerId, date)
                .orElseThrow(() -> new RuntimeException("Unavailable date not found"));
        unavailableDateRepository.delete(blockedDate);
    }

    @Override
    @Transactional
    public List<ProviderUnavailableDateResponse> getUnavailableDates(Long providerId) {
        repo.findById(providerId).orElseThrow(() -> new RuntimeException("Provider not found"));
        unavailableDateRepository.deleteByProviderIdAndDateBefore(providerId, LocalDate.now());
        return unavailableDateRepository.findByProviderIdOrderByDateAsc(providerId).stream()
                .map(this::mapUnavailableDate)
                .toList();
    }

    @Override
    public boolean isAvailableOn(Long providerId, LocalDate date) {
        Provider provider = repo.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        if (!provider.isAvailable()) {
            return false;
        }

        return !unavailableDateRepository.existsByProviderIdAndDate(providerId, date);
    }

    private ProviderResponse mapToResponse(Provider p) {
        String email = "";
        try {
            Map<?, ?> user = restTemplate.getForObject(
                    "http://auth-service/users/internal/" + p.getUserId(),
                    Map.class
            );
            email = user != null && user.get("email") != null ? user.get("email").toString() : "";
        } catch (Exception ignored) {
            email = "";
        }

        return ProviderResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .name(p.getName())
                .email(email)
                .imageUrl(p.getImageUrl())
                .specialization(p.getSpecialization())
                .qualification(p.getQualification())
                .experienceYears(p.getExperienceYears())
                .bio(p.getBio())
                .clinicName(p.getClinicName())
                .clinicAddress(p.getClinicAddress())
                .avgRating(p.getAvgRating())
                .minimumFees(p.getMinimumFees())
                .verified(p.isVerified())
                .available(p.isAvailable())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private ProviderUnavailableDateResponse mapUnavailableDate(ProviderUnavailableDate blockedDate) {
        return ProviderUnavailableDateResponse.builder()
                .id(blockedDate.getId())
                .providerId(blockedDate.getProviderId())
                .date(blockedDate.getDate())
                .reason(blockedDate.getReason())
                .build();
    }
}
