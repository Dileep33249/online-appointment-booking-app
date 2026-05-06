package com.app.appointment.repository;

import com.app.appointment.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByProviderIdAndIsBookedFalse(Long providerId);

    List<Slot> findByProviderId(Long providerId);

    List<Slot> findByIsBookedFalseAndStartTimeAfterOrderByStartTimeAsc(LocalDateTime time);

    List<Slot> findByProviderIdAndIsBookedFalseAndStartTimeAfterOrderByStartTimeAsc(Long providerId, LocalDateTime time);

    boolean existsByProviderIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long providerId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<Slot> findByProviderIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
            Long providerId,
            LocalDateTime endTime,
            LocalDateTime startTime
    );

    List<Slot> findByProviderIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            Long providerId,
            LocalDateTime start,
            LocalDateTime end
    );
}
