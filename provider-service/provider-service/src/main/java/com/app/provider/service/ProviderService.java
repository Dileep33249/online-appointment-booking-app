package com.app.provider.service;

import com.app.provider.dto.ProviderRequest;
import com.app.provider.dto.ProviderResponse;
import com.app.provider.dto.ProviderImageUpdateRequest;
import com.app.provider.dto.ProviderUnavailableDateRequest;
import com.app.provider.dto.ProviderUnavailableDateResponse;

import java.time.LocalDate;

import java.util.List;

public interface ProviderService {
    ProviderResponse addProvider(ProviderRequest request);
    ProviderResponse getById(Long id);
    ProviderResponse getByUserId(Long userId);
    List<ProviderResponse> getAll();
    List<ProviderResponse> getAllAdmin();
    List<ProviderResponse> search(String keyword);
    List<ProviderResponse> getBySpecialization(String specialization);
    ProviderResponse verifyProvider(Long id);
    ProviderResponse unverifyProvider(Long id);
    ProviderResponse updateMyProfile(Long userId, ProviderRequest request);
    ProviderResponse updateProviderProfileByAdmin(Long providerId, ProviderRequest request);
    ProviderResponse updateProviderImageByAdmin(Long providerId, ProviderImageUpdateRequest request);
    ProviderResponse updateAvailability(Long userId, boolean available);
    boolean isBookable(Long providerId);
    void deleteByUserId(Long userId);
    ProviderUnavailableDateResponse markUnavailableDate(Long providerId, ProviderUnavailableDateRequest request);
    void removeUnavailableDate(Long providerId, LocalDate date);
    List<ProviderUnavailableDateResponse> getUnavailableDates(Long providerId);
    boolean isAvailableOn(Long providerId, LocalDate date);
}
