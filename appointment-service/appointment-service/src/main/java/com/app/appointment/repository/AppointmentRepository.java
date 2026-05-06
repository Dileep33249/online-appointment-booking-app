package com.app.appointment.repository;

import com.app.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserId(Long userId);
    List<Appointment> findBySlotProviderId(Long providerId);
    List<Appointment> findBySlotId(Long slotId);
    boolean existsBySlotIdAndStatus(Long slotId, String status);
    List<Appointment> findBySlotIdAndStatusNotOrderByCreatedAtAsc(Long slotId, String status);

    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.slot.providerId = :providerId
              AND a.slot.startTime >= :dayStart
              AND a.slot.startTime < :dayEnd
              AND a.status <> :excludedStatus
            """)
    long countByProviderAndDayExcludingStatus(Long providerId,
                                              LocalDateTime dayStart,
                                              LocalDateTime dayEnd,
                                              String excludedStatus);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.slot.providerId = :providerId
              AND a.slot.startTime >= :dayStart
              AND a.slot.startTime < :dayEnd
              AND a.status <> :excludedStatus
            ORDER BY a.slot.startTime ASC, a.createdAt ASC, a.id ASC
            """)
    List<Appointment> findActiveAppointmentsForProviderDay(Long providerId,
                                                           LocalDateTime dayStart,
                                                           LocalDateTime dayEnd,
                                                           String excludedStatus);
}
