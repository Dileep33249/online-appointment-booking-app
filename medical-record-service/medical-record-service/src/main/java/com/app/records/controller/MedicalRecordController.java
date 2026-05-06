package com.app.records.controller;

import com.app.records.dto.*;
import com.app.records.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/records")
public class MedicalRecordController {
    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @PostMapping("/{appointmentId}")
    public ResponseEntity<MedicalRecordResponse> createRecord(@PathVariable Long appointmentId,
                                                              @Valid @RequestBody CreateMedicalRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalRecordService.createRecord(appointmentId, request));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MedicalRecordResponse>> getPatientHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicalRecordService.getPatientHistory(patientId));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<MedicalRecordResponse> getRecord(@PathVariable Long recordId) {
        return ResponseEntity.ok(medicalRecordService.getRecord(recordId));
    }

    @PostMapping("/{recordId}/attachments")
    public ResponseEntity<MedicalRecordResponse> addAttachment(@PathVariable Long recordId,
                                                               @Valid @RequestBody AttachmentRequest request) {
        return ResponseEntity.ok(medicalRecordService.addAttachment(recordId, request));
    }

    @PostMapping("/{recordId}/prescriptions")
    public ResponseEntity<MedicalRecordResponse> addPrescription(@PathVariable Long recordId,
                                                                 @Valid @RequestBody PrescriptionItemRequest request) {
        return ResponseEntity.ok(medicalRecordService.addPrescription(recordId, request));
    }
}
