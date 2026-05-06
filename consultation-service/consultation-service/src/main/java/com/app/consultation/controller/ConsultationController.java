package com.app.consultation.controller;

import com.app.consultation.dto.*;
import com.app.consultation.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultations")
public class ConsultationController {
    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    public ResponseEntity<ConsultationResponse> create(@Valid @RequestBody ConsultationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultationService.create(request));
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<ConsultationResponse> get(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(consultationService.getByAppointmentId(appointmentId));
    }

    @PutMapping("/{appointmentId}/start")
    public ResponseEntity<ConsultationResponse> start(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(consultationService.start(appointmentId));
    }

    @PutMapping("/{appointmentId}/complete")
    public ResponseEntity<ConsultationResponse> complete(@PathVariable Long appointmentId,
                                                         @RequestBody ConsultationCompletionRequest request) {
        return ResponseEntity.ok(consultationService.complete(appointmentId, request));
    }

    @PostMapping("/{appointmentId}/prescription")
    public ResponseEntity<ConsultationResponse> addPrescription(@PathVariable Long appointmentId,
                                                                @Valid @RequestBody PrescriptionSummaryRequest request) {
        return ResponseEntity.ok(consultationService.addPrescriptionSummary(appointmentId, request));
    }
}
