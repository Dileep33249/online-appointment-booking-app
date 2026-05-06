package com.app.appointment.service;

import com.app.appointment.dto.AppointmentRequest;
import com.app.appointment.dto.AppointmentResponse;
import com.app.appointment.dto.AppointmentNotificationEvent;
import com.app.appointment.dto.CreateNotificationRequest;
import com.app.appointment.dto.RescheduleAppointmentRequest;
import com.app.appointment.entity.Appointment;
import com.app.appointment.entity.Slot;
import com.app.appointment.repository.AppointmentRepository;
import com.app.appointment.repository.SlotRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentServiceImpl implements AppointmentService {
    private static final long MAX_DAILY_APPOINTMENTS_PER_DOCTOR = 100;

    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository,
                                  SlotRepository slotRepository,
                                  NotificationService notificationService,
                                  RestTemplate restTemplate) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.notificationService = notificationService;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "slotsAvailableByProvider", allEntries = true),
            @CacheEvict(value = "slotsByProvider", allEntries = true),
            @CacheEvict(value = "publicSlots", allEntries = true)
    })
    public AppointmentResponse bookSlot(Long slotId, AppointmentRequest request) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (slot.isBooked()) {
            throw new RuntimeException("This slot has already been booked");
        }

        if (slot.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot book past slots");
        }

        Boolean providerBookable = restTemplate.getForObject(
                "http://provider-service/providers/internal/" + slot.getProviderId() + "/available-on?date=" + slot.getStartTime().toLocalDate(),
                Boolean.class
        );

        if (providerBookable == null || !providerBookable) {
            throw new RuntimeException("This doctor is not available for booking on the selected date.");
        }

        LocalDateTime dayStart = slot.getStartTime().toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        long existingDailyAppointments = appointmentRepository.countByProviderAndDayExcludingStatus(
                slot.getProviderId(),
                dayStart,
                dayEnd,
                "CANCELLED"
        );

        if (existingDailyAppointments >= MAX_DAILY_APPOINTMENTS_PER_DOCTOR) {
            throw new RuntimeException("This doctor has reached the daily booking limit of 100 patients for the selected date.");
        }

        Appointment appointment = Appointment.builder()
                .userId(request.getUserId())
                .slot(slot)
                .status("BOOKED")
                .notes(request.getNotes())
                .build();

        appointment = appointmentRepository.save(appointment);
        slot.setBooked(true);
        slotRepository.save(slot);
        rebalanceProviderDayAppointments(slot.getProviderId(), slot.getStartTime());
        createBookingNotifications(appointment, slot);

        return mapToResponse(appointmentRepository.findById(appointment.getId()).orElse(appointment));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "slotsAvailableByProvider", allEntries = true),
            @CacheEvict(value = "slotsByProvider", allEntries = true),
            @CacheEvict(value = "publicSlots", allEntries = true)
    })
    public AppointmentResponse cancelAppointment(Long appointmentId, Long userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getUserId().equals(userId)) {
            throw new RuntimeException("You can cancel only your own appointment");
        }

        if (!"BOOKED".equals(appointment.getStatus())) {
            throw new RuntimeException("Only booked appointments can be cancelled");
        }

        appointment.setStatus("CANCELLED");

        Slot slot = appointment.getSlot();
        appointment.setQueueNumber(null);
        appointment.setEstimatedStartTime(null);
        appointment.setEstimatedEndTime(null);
        slot.setBooked(false);
        slotRepository.save(slot);

        Appointment saved = appointmentRepository.save(appointment);
        rebalanceProviderDayAppointments(slot.getProviderId(), slot.getStartTime());
        notificationService.sendCancellationNotification(buildNotificationEvent(
                saved,
                "CANCELLATION",
                "Appointment cancelled for " + slot.getStartTime().toLocalDate() + " at " + slot.getStartTime().toLocalTime()
        ));

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "slotsAvailableByProvider", allEntries = true),
            @CacheEvict(value = "slotsByProvider", allEntries = true),
            @CacheEvict(value = "publicSlots", allEntries = true)
    })
    public AppointmentResponse rescheduleAppointment(Long appointmentId, Long userId, RescheduleAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getUserId().equals(userId)) {
            throw new RuntimeException("You can reschedule only your own appointment");
        }

        if (!"BOOKED".equals(appointment.getStatus())) {
            throw new RuntimeException("Only booked appointments can be rescheduled");
        }

        Slot currentSlot = appointment.getSlot();
        Slot newSlot = slotRepository.findById(request.getNewSlotId())
                .orElseThrow(() -> new RuntimeException("New slot not found"));

        if (currentSlot.getId().equals(newSlot.getId())) {
            throw new RuntimeException("Please choose a different slot to reschedule");
        }

        if (newSlot.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot reschedule to a past slot");
        }

        Boolean providerBookable = restTemplate.getForObject(
                "http://provider-service/providers/internal/" + newSlot.getProviderId() + "/available-on?date=" + newSlot.getStartTime().toLocalDate(),
                Boolean.class
        );

        if (providerBookable == null || !providerBookable) {
            throw new RuntimeException("Provider is not available for booking");
        }

        if (newSlot.isBooked()) {
            throw new RuntimeException("Requested slot is already booked");
        }

        currentSlot.setBooked(false);
        slotRepository.save(currentSlot);
        newSlot.setBooked(true);
        slotRepository.save(newSlot);

        appointment.setSlot(newSlot);
        appointment.setStatus("RESCHEDULED");
        appointment.setEstimatedStartTime(newSlot.getStartTime());
        appointment.setEstimatedEndTime(newSlot.getEndTime());

        Appointment saved = appointmentRepository.save(appointment);
        rebalanceProviderDayAppointments(currentSlot.getProviderId(), currentSlot.getStartTime());
        if (!currentSlot.getStartTime().toLocalDate().equals(newSlot.getStartTime().toLocalDate())) {
            rebalanceProviderDayAppointments(newSlot.getProviderId(), newSlot.getStartTime());
        }
        notificationService.sendRescheduleNotification(buildNotificationEvent(
                saved,
                "RESCHEDULED",
                "Appointment rescheduled to " + newSlot.getStartTime().toLocalDate() + " at " + newSlot.getStartTime().toLocalTime()
        ));
        return mapToResponse(appointmentRepository.findById(saved.getId()).orElse(saved));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "slotsAvailableByProvider", allEntries = true),
            @CacheEvict(value = "slotsByProvider", allEntries = true),
            @CacheEvict(value = "publicSlots", allEntries = true)
    })
    public AppointmentResponse completeAppointment(Long appointmentId, Long providerId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getSlot().getProviderId().equals(providerId)) {
            throw new RuntimeException("You can complete only your own appointments");
        }

        if (!"BOOKED".equals(appointment.getStatus()) && !"RESCHEDULED".equals(appointment.getStatus())) {
            throw new RuntimeException("Only active appointments can be completed");
        }

        appointment.setStatus("COMPLETED");
        appointment.setCompletedAt(LocalDateTime.now());
        Appointment saved = appointmentRepository.save(appointment);
        notificationService.sendCompletionNotification(buildNotificationEvent(
                saved,
                "COMPLETED",
                "Appointment completed on " + appointment.getCompletedAt().toLocalDate()
        ));
        return mapToResponse(saved);
    }

    @Override
    public List<AppointmentResponse> getMyAppointments(Long userId) {
        return appointmentRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponse> getProviderAppointments(Long providerId) {
        return appointmentRepository.findBySlotProviderId(providerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void rebalanceProviderDayAppointments(Long providerId, LocalDateTime slotStartTime) {
        LocalDateTime dayStart = slotStartTime.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        List<Appointment> activeAppointments = appointmentRepository.findActiveAppointmentsForProviderDay(
                providerId,
                dayStart,
                dayEnd,
                "CANCELLED"
        );

        for (int index = 0; index < activeAppointments.size(); index++) {
            Appointment activeAppointment = activeAppointments.get(index);
            Slot activeSlot = activeAppointment.getSlot();
            activeAppointment.setQueueNumber(index + 1);
            activeAppointment.setEstimatedStartTime(activeSlot.getStartTime());
            activeAppointment.setEstimatedEndTime(activeSlot.getEndTime());
        }

        if (!activeAppointments.isEmpty()) {
            appointmentRepository.saveAll(activeAppointments);
        }

        List<Slot> providerSlots = slotRepository.findByProviderIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                providerId,
                dayStart,
                dayEnd
        );

        for (Slot providerSlot : providerSlots) {
            boolean hasActiveAppointment = activeAppointments.stream()
                    .anyMatch(appointment -> appointment.getSlot().getId().equals(providerSlot.getId()));
            providerSlot.setBooked(hasActiveAppointment);
        }

        if (!providerSlots.isEmpty()) {
            slotRepository.saveAll(providerSlots);
        }
    }

    private AppointmentNotificationEvent buildNotificationEvent(Appointment appointment, String type, String message) {
        return AppointmentNotificationEvent.builder()
                .userId(appointment.getUserId())
                .appointmentId(appointment.getId())
                .type(type)
                .channel("IN_APP")
                .message(message)
                .scheduledFor(LocalDateTime.now())
                .build();
    }

    private void createBookingNotifications(Appointment appointment, Slot slot) {
        String patientMessage = "Your appointment was booked successfully for "
                + slot.getStartTime().toLocalDate() + " at " + slot.getStartTime().toLocalTime() + ".";
        notificationService.sendNotification(CreateNotificationRequest.builder()
                .userId(appointment.getUserId())
                .appointmentId(appointment.getId())
                .type("BOOKING_CONFIRMATION")
                .channel("IN_APP")
                .message(patientMessage)
                .scheduledFor(LocalDateTime.now())
                .build());

        try {
            Map<?, ?> provider = restTemplate.getForObject(
                    "http://provider-service/providers/" + slot.getProviderId(),
                    Map.class
            );

            if (provider != null && provider.get("userId") != null) {
                Long doctorUserId = Long.valueOf(provider.get("userId").toString());
                String patientName = resolvePatientName(appointment.getUserId());
                String doctorMessage = patientName + " booked an appointment for "
                        + slot.getStartTime().toLocalDate() + " at " + slot.getStartTime().toLocalTime() + ".";

                notificationService.sendNotification(CreateNotificationRequest.builder()
                        .userId(doctorUserId)
                        .appointmentId(appointment.getId())
                        .type("NEW_APPOINTMENT")
                        .channel("IN_APP")
                        .message(doctorMessage)
                        .scheduledFor(LocalDateTime.now())
                        .build());
            }
        } catch (Exception exception) {
            System.err.println("Could not create doctor notification for appointment " + appointment.getId());
        }
    }

    private String resolvePatientName(Long userId) {
        try {
            Object userResponse = restTemplate.getForObject(
                    "http://auth-service/users/internal/" + userId,
                    Object.class
            );
            if (userResponse instanceof java.util.Map<?, ?> userMap) {
                Object name = userMap.get("name");
                if (name != null && !name.toString().isBlank()) {
                    return name.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return "A patient";
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        Slot slot = appointment.getSlot();
        
        // Fetch patient name from auth service
        String patientName = resolvePatientName(appointment.getUserId());
        String doctorName = resolveDoctorName(slot.getProviderId());
        double providerFee = resolveProviderFee(slot.getProviderId());

        return AppointmentResponse.builder()
                .id(appointment.getId())
                .userId(appointment.getUserId())
                .patientName(patientName)
                .doctorName(doctorName)
                .slotId(slot.getId())
                .providerId(slot.getProviderId())
                .providerFee(providerFee)
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .queueNumber(appointment.getQueueNumber())
                .slotStartTime(slot.getStartTime())
                .slotEndTime(slot.getEndTime())
                .estimatedStartTime(appointment.getEstimatedStartTime())
                .estimatedEndTime(appointment.getEstimatedEndTime())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .completedAt(appointment.getCompletedAt())
                .build();
    }

    private String resolveDoctorName(Long providerId) {
        try {
            Map<?, ?> providerResponse = restTemplate.getForObject(
                    "http://provider-service/providers/" + providerId,
                    Map.class
            );
            if (providerResponse != null) {
                if (providerResponse.get("name") != null) {
                    String name = providerResponse.get("name").toString();
                    if (!name.isBlank()) {
                        return name;
                    }
                }

                if (providerResponse.get("userId") != null) {
                    String accountName = resolveUserName(Long.valueOf(providerResponse.get("userId").toString()));
                    if (!accountName.isBlank()) {
                        return accountName;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Doctor";
    }

    private String resolveUserName(Long userId) {
        try {
            Object userResponse = restTemplate.getForObject(
                    "http://auth-service/users/internal/" + userId,
                    Object.class
            );
            if (userResponse instanceof java.util.Map<?, ?> userMap) {
                Object name = userMap.get("name");
                if (name != null && !name.toString().isBlank()) {
                    return name.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private double resolveProviderFee(Long providerId) {
        try {
            Map<?, ?> providerResponse = restTemplate.getForObject(
                    "http://provider-service/providers/" + providerId,
                    Map.class
            );
            if (providerResponse != null && providerResponse.get("minimumFees") != null) {
                return Double.parseDouble(providerResponse.get("minimumFees").toString());
            }
        } catch (Exception ignored) {
        }
        return 0.0;
    }
}
