package com.app.appointment.service;

import com.app.appointment.dto.SlotRequest;
import com.app.appointment.dto.SlotResponse;
import com.app.appointment.repository.AppointmentRepository;
import com.app.appointment.entity.Slot;
import com.app.appointment.repository.SlotRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlotServiceImpl implements SlotService {
    private static final LocalTime DUTY_START = LocalTime.of(10, 0);
    private static final LocalTime DUTY_END = LocalTime.of(17, 0);
    private static final LocalTime LUNCH_START = LocalTime.of(13, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(14, 0);
    private static final int SLOT_LENGTH_MINUTES = 10;
    private static final int MINIMUM_DUTY_BLOCK_MINUTES = 60;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final RestTemplate restTemplate;

    public SlotServiceImpl(SlotRepository slotRepository,
                           AppointmentRepository appointmentRepository,
                           RestTemplate restTemplate) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "slotsAvailableByProvider", allEntries = true),
            @CacheEvict(value = "slotsByProvider", allEntries = true),
            @CacheEvict(value = "publicSlots", allEntries = true)
    })
    public SlotResponse createSlot(SlotRequest request) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new RuntimeException("Start time must be before end time");
        }

        if (!request.getStartTime().toLocalDate().equals(request.getEndTime().toLocalDate())) {
            throw new RuntimeException("Duty slots must be created within the same day");
        }

        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot create slots in the past");
        }

        if (request.getStartTime().toLocalTime().isBefore(DUTY_START) || request.getEndTime().toLocalTime().isAfter(DUTY_END)) {
            throw new RuntimeException("Doctor duty slots can only be created between 10:00 AM and 5:00 PM");
        }

        long requestedMinutes = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (requestedMinutes < MINIMUM_DUTY_BLOCK_MINUTES) {
            throw new RuntimeException("Please create availability for at least 1 hour");
        }

        try {
            restTemplate.getForObject(
                    "http://provider-service/providers/" + request.getProviderId(),
                    Map.class
            );
        } catch (Exception ex) {
            throw new RuntimeException("Provider profile not found");
        }

        List<Slot> generatedSlots = new ArrayList<>();
        LocalDateTime currentStart = request.getStartTime();
        LocalDateTime finalEnd = request.getEndTime();

        while (currentStart.isBefore(finalEnd)) {
            LocalDateTime currentEnd = currentStart.plusMinutes(SLOT_LENGTH_MINUTES);
            if (currentEnd.isAfter(finalEnd)) {
                break;
            }

            LocalTime slotStartTime = currentStart.toLocalTime();
            LocalTime slotEndTime = currentEnd.toLocalTime();
            boolean overlapsLunch = slotStartTime.isBefore(LUNCH_END) && slotEndTime.isAfter(LUNCH_START);

            if (!overlapsLunch) {
                List<Slot> overlappingSlots = slotRepository.findByProviderIdAndStartTimeLessThanAndEndTimeGreaterThanOrderByStartTimeAsc(
                        request.getProviderId(),
                        currentEnd,
                        currentStart
                );

                if (!overlappingSlots.isEmpty()) {
                    String conflictingTimes = overlappingSlots.stream()
                            .map(slot -> formatTimeRange(slot.getStartTime(), slot.getEndTime()))
                            .distinct()
                            .collect(Collectors.joining(", "));
                    throw new RuntimeException("Overlapping slot already exists on this day: " + conflictingTimes);
                }

                generatedSlots.add(Slot.builder()
                        .providerId(request.getProviderId())
                        .startTime(currentStart)
                        .endTime(currentEnd)
                        .isBooked(false)
                        .build());
            }

            currentStart = currentEnd;
        }

        if (generatedSlots.isEmpty()) {
            throw new RuntimeException("No valid 10-minute slots were generated. Lunch break from 1 PM to 2 PM is skipped automatically.");
        }

        List<Slot> savedSlots = slotRepository.saveAll(generatedSlots);
        return mapToResponse(savedSlots.get(0));
    }

    @Override
    @Cacheable(value = "slotsAvailableByProvider", key = "#providerId")
    public List<SlotResponse> getAvailableSlots(Long providerId) {
        Boolean providerBookable = restTemplate.getForObject(
                "http://provider-service/providers/internal/" + providerId + "/bookable",
                Boolean.class
        );

        if (providerBookable == null || !providerBookable) {
            return Collections.emptyList();
        }

        return slotRepository.findByProviderIdAndIsBookedFalseAndStartTimeAfterOrderByStartTimeAsc(
                        providerId,
                        LocalDateTime.now()
                )
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "slotsByProvider", key = "#providerId")
    public List<SlotResponse> getAllSlotsOfProvider(Long providerId) {
        return slotRepository.findByProviderId(providerId)
                .stream()
                .filter(slot -> !slot.getEndTime().isBefore(LocalDateTime.now()))
                .sorted(Comparator.comparing(Slot::getStartTime))
                .map(this::mapToResponseUnfiltered)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "publicSlots", key = "T(java.lang.String).valueOf(#date) + '|' + (#specialization == null ? '' : #specialization.toLowerCase()) + '|' + (#search == null ? '' : #search.toLowerCase())")
    public List<SlotResponse> getPublicAvailableSlots(LocalDate date, String specialization, String search) {
        if (date != null) {
            LocalDate today = LocalDate.now();
            LocalDate maxDate = today.plusDays(13);
            if (date.isBefore(today) || date.isAfter(maxDate)) {
                throw new RuntimeException("You can search doctor availability only within the next two weeks");
            }
        }

        List<Slot> slots = slotRepository.findByIsBookedFalseAndStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now());

        return slots.stream()
                .map(slot -> enrichSlot(slot))
                .filter(Objects::nonNull)
                .filter(slot -> {
                    if (date == null) return true;
                    return slot.getStartTime().toLocalDate().equals(date);
                })
                .filter(slot -> {
                    if (specialization == null || specialization.isBlank()) return true;
                    return slot.getSpecialization() != null
                            && slot.getSpecialization().equalsIgnoreCase(specialization.trim());
                })
                .filter(slot -> {
                    if (search == null || search.isBlank()) return true;
                    String term = search.toLowerCase();
                    return (slot.getDoctorName() != null && slot.getDoctorName().toLowerCase().contains(term)) ||
                           (slot.getSpecialization() != null && slot.getSpecialization().toLowerCase().contains(term)) ||
                           (slot.getClinicName() != null && slot.getClinicName().toLowerCase().contains(term));
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "slotsAvailableByProvider", allEntries = true),
            @CacheEvict(value = "slotsByProvider", allEntries = true),
            @CacheEvict(value = "publicSlots", allEntries = true)
    })
    public void deleteSlot(Long slotId, Long providerId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.getProviderId().equals(providerId)) {
            throw new RuntimeException("You do not have permission to delete this slot");
        }

        if (slot.isBooked() || appointmentRepository.existsBySlotIdAndStatus(slotId, "BOOKED")) {
            throw new RuntimeException("Cannot delete a booked slot");
        }

        appointmentRepository.deleteAll(appointmentRepository.findBySlotId(slotId));
        slotRepository.delete(slot);
    }

    @Override
    public boolean isProviderAvailableOn(Long providerId, LocalDate date) {
        Boolean providerAvailable = restTemplate.getForObject(
                "http://provider-service/providers/internal/" + providerId + "/available-on?date=" + date,
                Boolean.class
        );

        if (providerAvailable == null || !providerAvailable) {
            return false;
        }

        return slotRepository.findByProviderIdAndIsBookedFalseAndStartTimeAfterOrderByStartTimeAsc(
                        providerId,
                        LocalDateTime.now()
                )
                .stream()
                .anyMatch(slot -> slot.getStartTime().toLocalDate().equals(date));
    }

    private SlotResponse mapToResponse(Slot slot) {
        return enrichSlot(slot);
    }

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        return start.toLocalTime().format(TIME_FORMATTER) + " - " + end.toLocalTime().format(TIME_FORMATTER);
    }

    private SlotResponse enrichSlot(Slot slot) {
        try {
            Boolean providerBookable = restTemplate.getForObject(
                    "http://provider-service/providers/internal/" + slot.getProviderId() + "/available-on?date=" + slot.getStartTime().toLocalDate(),
                    Boolean.class
            );

            if (providerBookable == null || !providerBookable) {
                return null;
            }

            return mapToResponseUnfiltered(slot);
        } catch (Exception ex) {
            return null;
        }
    }

    private SlotResponse mapToResponseUnfiltered(Slot slot) {
        try {
            Map<?, ?> provider = restTemplate.getForObject(
                    "http://provider-service/providers/" + slot.getProviderId(),
                    Map.class
            );

            String doctorName = provider != null ? Objects.toString(provider.get("name"), "") : "";
            String specialization = provider != null ? Objects.toString(provider.get("specialization"), "") : "";
            String clinicName = provider != null ? Objects.toString(provider.get("clinicName"), "") : "";
            double minimumFees = 0.0;
            if (provider != null && provider.get("minimumFees") != null) {
                try {
                    minimumFees = Double.parseDouble(provider.get("minimumFees").toString());
                } catch (NumberFormatException e) {
                    minimumFees = 0.0;
                }
            }

            return SlotResponse.builder()
                    .id(slot.getId())
                    .providerId(slot.getProviderId())
                    .doctorName(doctorName)
                    .specialization(specialization)
                    .clinicName(clinicName)
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .isBooked(slot.isBooked())
                    .minimumFees(minimumFees)
                    .build();
        } catch (Exception ex) {
            return SlotResponse.builder()
                    .id(slot.getId())
                    .providerId(slot.getProviderId())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .isBooked(slot.isBooked())
                    .minimumFees(0.0)
                    .build();
        }
    }
}
