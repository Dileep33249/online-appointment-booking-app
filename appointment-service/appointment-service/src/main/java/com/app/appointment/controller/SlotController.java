package com.app.appointment.controller;

import com.app.appointment.dto.SlotRequest;
import com.app.appointment.dto.SlotResponse;
import com.app.appointment.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slots")
public class SlotController {

    private final SlotService slotService;
    private final RestTemplate restTemplate;

    public SlotController(SlotService slotService, RestTemplate restTemplate) {
        this.slotService = slotService;
        this.restTemplate = restTemplate;
    }

    @PostMapping
    public ResponseEntity<SlotResponse> createSlot(@Valid @RequestBody SlotRequest request,
                                                   Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        Map<?, ?> provider = restTemplate.getForObject(
                "http://provider-service/providers/internal/by-user/" + userId,
                Map.class
        );

        if (provider == null || provider.get("id") == null) {
            throw new RuntimeException("Doctor provider profile not found");
        }

        request.setProviderId(Long.valueOf(provider.get("id").toString()));
        return ResponseEntity.status(HttpStatus.CREATED).body(slotService.createSlot(request));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<SlotResponse>> getAvailableSlots(@PathVariable Long providerId) {
        return ResponseEntity.ok(slotService.getAvailableSlots(providerId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<SlotResponse>> getPublicSlots(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(slotService.getPublicAvailableSlots(date, specialization, search));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        Map<?, ?> provider = restTemplate.getForObject(
                "http://provider-service/providers/internal/by-user/" + userId,
                Map.class
        );

        if (provider == null || provider.get("id") == null) {
            throw new RuntimeException("Doctor provider profile not found");
        }

        Long providerId = Long.valueOf(provider.get("id").toString());
        slotService.deleteSlot(id, providerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<SlotResponse>> getMySlots(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        Map<?, ?> provider = restTemplate.getForObject(
                "http://provider-service/providers/internal/by-user/" + userId,
                Map.class
        );

        if (provider == null || provider.get("id") == null) {
            throw new RuntimeException("Doctor provider profile not found");
        }

        Long providerId = Long.valueOf(provider.get("id").toString());
        return ResponseEntity.ok(slotService.getAllSlotsOfProvider(providerId));
    }
}
