package com.app.provider.repository;

import com.app.provider.entity.ProviderUnavailableDate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProviderUnavailableDateRepository extends JpaRepository<ProviderUnavailableDate, Long> {
    boolean existsByProviderIdAndDate(Long providerId, LocalDate date);
    Optional<ProviderUnavailableDate> findByProviderIdAndDate(Long providerId, LocalDate date);
    List<ProviderUnavailableDate> findByProviderIdOrderByDateAsc(Long providerId);
    void deleteByProviderIdAndDateBefore(Long providerId, LocalDate date);
    void deleteByProviderId(Long providerId);
}
