package com.app.provider.controller;

import com.app.provider.dto.ProviderRequest;
import com.app.provider.dto.ProviderResponse;
import com.app.provider.dto.ProviderImageUpdateRequest;
import com.app.provider.dto.ProviderUnavailableDateRequest;
import com.app.provider.dto.ProviderUnavailableDateResponse;
import com.app.provider.service.ProviderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/providers")
public class ProviderController {

    private final ProviderService service;

    public ProviderController(ProviderService service) {
        this.service = service;
    }

    @PostMapping("/add")
    public ResponseEntity<ProviderResponse> add(@Valid @RequestBody ProviderRequest request,
                                                Authentication authentication) {
        Long loggedInUserId = (Long) authentication.getPrincipal();
        request.setUserId(loggedInUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addProvider(request));
    }

    @GetMapping
    public ResponseEntity<List<ProviderResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProviderResponse>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(service.search(keyword));
    }

    @GetMapping("/specialization")
    public ResponseEntity<List<ProviderResponse>> getBySpecialization(@RequestParam String name) {
        return ResponseEntity.ok(service.getBySpecialization(name));
    }

    @GetMapping("/me")
    public ResponseEntity<ProviderResponse> getMyProfile(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<ProviderResponse> updateMyProfile(@Valid @RequestBody ProviderRequest request,
                                                            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(service.updateMyProfile(userId, request));
    }

    @PutMapping("/me/availability")
    public ResponseEntity<ProviderResponse> updateAvailability(@RequestParam boolean available,
                                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(service.updateAvailability(userId, available));
    }

    @GetMapping("/me/unavailable-dates")
    public ResponseEntity<List<ProviderUnavailableDateResponse>> getMyUnavailableDates(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ProviderResponse provider = service.getByUserId(userId);
        return ResponseEntity.ok(service.getUnavailableDates(provider.getId()));
    }

    @PostMapping("/me/unavailable-dates")
    public ResponseEntity<ProviderUnavailableDateResponse> markMyUnavailableDate(@Valid @RequestBody ProviderUnavailableDateRequest request,
                                                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ProviderResponse provider = service.getByUserId(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.markUnavailableDate(provider.getId(), request));
    }

    @DeleteMapping("/me/unavailable-dates")
    public ResponseEntity<Void> removeMyUnavailableDate(@RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date,
                                                        Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ProviderResponse provider = service.getByUserId(userId);
        service.removeUnavailableDate(provider.getId(), date);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<ProviderResponse> verify(@PathVariable Long id) {
        return ResponseEntity.ok(service.verifyProvider(id));
    }

    @PutMapping("/{id}/unverify")
    public ResponseEntity<ProviderResponse> unverify(@PathVariable Long id) {
        return ResponseEntity.ok(service.unverifyProvider(id));
    }

    @PutMapping("/{id}/admin-profile")
    public ResponseEntity<ProviderResponse> updateProfileByAdmin(@PathVariable Long id,
                                                                 @Valid @RequestBody ProviderRequest request) {
        return ResponseEntity.ok(service.updateProviderProfileByAdmin(id, request));
    }

    @PutMapping("/{id}/admin-image")
    public ResponseEntity<ProviderResponse> updateImageByAdmin(@PathVariable Long id,
                                                               @Valid @RequestBody ProviderImageUpdateRequest request) {
        return ResponseEntity.ok(service.updateProviderImageByAdmin(id, request));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<ProviderResponse>> getAllAdmin() {
        return ResponseEntity.ok(service.getAllAdmin());
    }

    @GetMapping("/{id}/unavailable-dates")
    public ResponseEntity<List<ProviderUnavailableDateResponse>> getUnavailableDates(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUnavailableDates(id));
    }

    @PostMapping("/{id}/unavailable-dates")
    public ResponseEntity<ProviderUnavailableDateResponse> markUnavailableDate(@PathVariable Long id,
                                                                               @Valid @RequestBody ProviderUnavailableDateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.markUnavailableDate(id, request));
    }

    @DeleteMapping("/{id}/unavailable-dates")
    public ResponseEntity<Void> removeUnavailableDate(@PathVariable Long id, @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        service.removeUnavailableDate(id, date);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/by-user/{userId}")
    public ResponseEntity<ProviderResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByUserId(userId));
    }

    @DeleteMapping("/internal/by-user/{userId}")
    public ResponseEntity<Void> deleteByUserId(@PathVariable Long userId) {
        service.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/{providerId}/bookable")
    public ResponseEntity<Boolean> isBookable(@PathVariable Long providerId) {
        return ResponseEntity.ok(service.isBookable(providerId));
    }

    @GetMapping("/internal/{providerId}/available-on")
    public ResponseEntity<Boolean> isAvailableOn(@PathVariable Long providerId, @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.isAvailableOn(providerId, date));
    }
}
