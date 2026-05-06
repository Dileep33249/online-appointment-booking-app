package com.app.appointment.controller;

import com.app.appointment.dto.AppointmentRequest;
import com.app.appointment.dto.AppointmentResponse;
import com.app.appointment.dto.RescheduleAppointmentRequest;
import com.app.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final RestTemplate restTemplate;

    public AppointmentController(AppointmentService appointmentService, RestTemplate restTemplate) {
        this.appointmentService = appointmentService;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/book/{slotId}")
    public ResponseEntity<AppointmentResponse> bookSlot(@PathVariable Long slotId,
                                                        @Valid @RequestBody AppointmentRequest request,
                                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        request.setUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookSlot(slotId, request));
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable Long appointmentId,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(appointmentService.cancelAppointment(appointmentId, userId));
    }

    @PutMapping("/{appointmentId}/reschedule")
    public ResponseEntity<AppointmentResponse> reschedule(@PathVariable Long appointmentId,
                                                          @Valid @RequestBody RescheduleAppointmentRequest request,
                                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(appointmentId, userId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponse>> myAppointments(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(appointmentService.getMyAppointments(userId));
    }

    @GetMapping("/provider")
    public ResponseEntity<List<AppointmentResponse>> providerAppointments(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        Map<?, ?> provider = restTemplate.getForObject(
                "http://provider-service/providers/internal/by-user/" + userId,
                Map.class
        );

        if (provider == null || provider.get("id") == null) {
            throw new RuntimeException("Doctor provider profile not found");
        }

        Long providerId = Long.valueOf(provider.get("id").toString());
        return ResponseEntity.ok(appointmentService.getProviderAppointments(providerId));
    }

    @PutMapping("/{appointmentId}/complete")
    public ResponseEntity<AppointmentResponse> complete(@PathVariable Long appointmentId,
                                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        Map<?, ?> provider = restTemplate.getForObject(
                "http://provider-service/providers/internal/by-user/" + userId,
                Map.class
        );

        if (provider == null || provider.get("id") == null) {
            throw new RuntimeException("Doctor provider profile not found");
        }

        Long providerId = Long.valueOf(provider.get("id").toString());
        return ResponseEntity.ok(appointmentService.completeAppointment(appointmentId, providerId));
    }
}
