package com.app.consultation.service;

import com.app.consultation.dto.*;
import com.app.consultation.entity.Consultation;
import com.app.consultation.repository.ConsultationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ConsultationService {
    private final ConsultationRepository consultationRepository;

    public ConsultationService(ConsultationRepository consultationRepository) {
        this.consultationRepository = consultationRepository;
    }

    public ConsultationResponse create(ConsultationRequest request) {
        consultationRepository.findByAppointmentId(request.getAppointmentId()).ifPresent(existing -> {
            throw new RuntimeException("Consultation already exists for this appointment");
        });

        Consultation consultation = Consultation.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .mode(request.getMode().toUpperCase())
                .status("SCHEDULED")
                .meetingLink(request.getMeetingLink())
                .symptoms(request.getSymptoms())
                .notes(request.getNotes())
                .build();

        return map(consultationRepository.save(consultation));
    }

    public ConsultationResponse getByAppointmentId(Long appointmentId) {
        return map(load(appointmentId));
    }

    public ConsultationResponse start(Long appointmentId) {
        Consultation consultation = load(appointmentId);
        consultation.setStatus("IN_PROGRESS");
        consultation.setStartedAt(LocalDateTime.now());
        return map(consultationRepository.save(consultation));
    }

    public ConsultationResponse complete(Long appointmentId, ConsultationCompletionRequest request) {
        Consultation consultation = load(appointmentId);
        consultation.setStatus("COMPLETED");
        consultation.setDiagnosis(request.getDiagnosis());
        consultation.setNotes(request.getNotes());
        consultation.setCompletedAt(LocalDateTime.now());
        return map(consultationRepository.save(consultation));
    }

    public ConsultationResponse addPrescriptionSummary(Long appointmentId, PrescriptionSummaryRequest request) {
        Consultation consultation = load(appointmentId);
        consultation.setPrescriptionSummary(request.getPrescriptionSummary());
        return map(consultationRepository.save(consultation));
    }

    private Consultation load(Long appointmentId) {
        return consultationRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Consultation not found for appointment: " + appointmentId));
    }

    private ConsultationResponse map(Consultation consultation) {
        return ConsultationResponse.builder()
                .id(consultation.getId())
                .appointmentId(consultation.getAppointmentId())
                .patientId(consultation.getPatientId())
                .providerId(consultation.getProviderId())
                .mode(consultation.getMode())
                .status(consultation.getStatus())
                .meetingLink(consultation.getMeetingLink())
                .symptoms(consultation.getSymptoms())
                .diagnosis(consultation.getDiagnosis())
                .notes(consultation.getNotes())
                .prescriptionSummary(consultation.getPrescriptionSummary())
                .startedAt(consultation.getStartedAt())
                .completedAt(consultation.getCompletedAt())
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }
}
