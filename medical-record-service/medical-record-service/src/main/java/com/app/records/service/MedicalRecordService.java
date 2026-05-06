package com.app.records.service;

import com.app.records.dto.*;
import com.app.records.entity.MedicalRecord;
import com.app.records.entity.PrescriptionItem;
import com.app.records.entity.RecordAttachment;
import com.app.records.repository.MedicalRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
    }

    public MedicalRecordResponse createRecord(Long appointmentId, CreateMedicalRecordRequest request) {
        medicalRecordRepository.findByAppointmentId(appointmentId).ifPresent(record -> {
            throw new RuntimeException("Medical record already exists for this appointment");
        });

        MedicalRecord medicalRecord = MedicalRecord.builder()
                .appointmentId(appointmentId)
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .diagnosis(request.getDiagnosis())
                .consultationNotes(request.getConsultationNotes())
                .build();

        return map(medicalRecordRepository.save(medicalRecord));
    }

    public List<MedicalRecordResponse> getPatientHistory(Long patientId) {
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(this::map).toList();
    }

    public MedicalRecordResponse getRecord(Long recordId) {
        return map(loadByRecordId(recordId));
    }

    public MedicalRecordResponse addAttachment(Long recordId, AttachmentRequest request) {
        MedicalRecord record = loadByRecordId(recordId);
        record.getAttachments().add(RecordAttachment.builder()
                .medicalRecord(record)
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .fileType(request.getFileType())
                .build());
        return map(medicalRecordRepository.save(record));
    }

    public MedicalRecordResponse addPrescription(Long recordId, PrescriptionItemRequest request) {
        MedicalRecord record = loadByRecordId(recordId);
        record.getPrescriptions().add(PrescriptionItem.builder()
                .medicalRecord(record)
                .medicineName(request.getMedicineName())
                .dosage(request.getDosage())
                .frequency(request.getFrequency())
                .duration(request.getDuration())
                .build());
        return map(medicalRecordRepository.save(record));
    }

    private MedicalRecord loadByRecordId(Long recordId) {
        return medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Medical record not found: " + recordId));
    }

    private MedicalRecordResponse map(MedicalRecord record) {
        return MedicalRecordResponse.builder()
                .id(record.getId())
                .appointmentId(record.getAppointmentId())
                .patientId(record.getPatientId())
                .providerId(record.getProviderId())
                .diagnosis(record.getDiagnosis())
                .consultationNotes(record.getConsultationNotes())
                .prescriptions(record.getPrescriptions().stream()
                        .map(item -> PrescriptionItemRequest.builder()
                                .medicineName(item.getMedicineName())
                                .dosage(item.getDosage())
                                .frequency(item.getFrequency())
                                .duration(item.getDuration())
                                .build())
                        .toList())
                .attachments(record.getAttachments().stream()
                        .map(item -> AttachmentRequest.builder()
                                .fileName(item.getFileName())
                                .fileUrl(item.getFileUrl())
                                .fileType(item.getFileType())
                                .build())
                        .toList())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
