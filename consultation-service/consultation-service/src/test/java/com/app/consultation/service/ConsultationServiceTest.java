package com.app.consultation.service;

import com.app.consultation.dto.*;
import com.app.consultation.entity.Consultation;
import com.app.consultation.repository.ConsultationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @InjectMocks
    private ConsultationService consultationService;

    private Consultation mockConsultation;

    @BeforeEach
    void setUp() {
        mockConsultation = Consultation.builder()
                .id(1L)
                .appointmentId(10L)
                .patientId(100L)
                .providerId(200L)
                .mode("VIDEO")
                .status("SCHEDULED")
                .symptoms("Cough and fever")
                .notes("Needs rest")
                .build();
    }

    @Test
    void testCreateConsultationSuccess() {
        ConsultationRequest request = new ConsultationRequest();
        request.setAppointmentId(10L);
        request.setPatientId(100L);
        request.setProviderId(200L);
        request.setMode("VIDEO");
        request.setSymptoms("Cough and fever");
        request.setNotes("Needs rest");

        when(consultationRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
        when(consultationRepository.save(any(Consultation.class))).thenReturn(mockConsultation);

        ConsultationResponse response = consultationService.create(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("SCHEDULED", response.getStatus());
        verify(consultationRepository, times(1)).findByAppointmentId(10L);
        verify(consultationRepository, times(1)).save(any(Consultation.class));
    }

    @Test
    void testCreateConsultationAlreadyExists() {
        ConsultationRequest request = new ConsultationRequest();
        request.setAppointmentId(10L);

        when(consultationRepository.findByAppointmentId(10L)).thenReturn(Optional.of(mockConsultation));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> consultationService.create(request));
        assertEquals("Consultation already exists for this appointment", exception.getMessage());
        verify(consultationRepository, never()).save(any(Consultation.class));
    }

    @Test
    void testGetByAppointmentId() {
        when(consultationRepository.findByAppointmentId(10L)).thenReturn(Optional.of(mockConsultation));

        ConsultationResponse response = consultationService.getByAppointmentId(10L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("VIDEO", response.getMode());
        verify(consultationRepository, times(1)).findByAppointmentId(10L);
    }

    @Test
    void testStartConsultation() {
        when(consultationRepository.findByAppointmentId(10L)).thenReturn(Optional.of(mockConsultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationResponse response = consultationService.start(10L);

        assertNotNull(response);
        assertEquals("IN_PROGRESS", response.getStatus());
        assertNotNull(response.getStartedAt());
        verify(consultationRepository, times(1)).findByAppointmentId(10L);
        verify(consultationRepository, times(1)).save(any(Consultation.class));
    }

    @Test
    void testCompleteConsultation() {
        ConsultationCompletionRequest request = new ConsultationCompletionRequest();
        request.setDiagnosis("Common Cold");
        request.setSymptoms("Mild cough");
        request.setNotes("Prescribed paracetamol");
        request.setPrescriptionSummary("Paracetamol 500mg daily");

        when(consultationRepository.findByAppointmentId(10L)).thenReturn(Optional.of(mockConsultation));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationResponse response = consultationService.complete(10L, request);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("Common Cold", response.getDiagnosis());
        assertEquals("Paracetamol 500mg daily", response.getPrescriptionSummary());
        assertNotNull(response.getCompletedAt());
        verify(consultationRepository, times(1)).findByAppointmentId(10L);
    }
}
