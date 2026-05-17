package com.app.records.service;

import com.app.records.dto.*;
import com.app.records.entity.MedicalRecord;
import com.app.records.repository.MedicalRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @InjectMocks
    private MedicalRecordService medicalRecordService;

    private MedicalRecord mockRecord;

    @BeforeEach
    void setUp() {
        mockRecord = MedicalRecord.builder()
                .id(1L)
                .appointmentId(10L)
                .patientId(100L)
                .providerId(200L)
                .diagnosis("Common Flu")
                .consultationNotes("Take rest and drink warm water")
                .symptoms("Cough, sore throat")
                .prescriptions(new ArrayList<>())
                .attachments(new ArrayList<>())
                .build();
    }

    @Test
    void testCreateRecordSuccess() {
        CreateMedicalRecordRequest request = new CreateMedicalRecordRequest();
        request.setPatientId(100L);
        request.setProviderId(200L);
        request.setDiagnosis("Common Flu");
        request.setConsultationNotes("Take rest and drink warm water");
        request.setSymptoms("Cough, sore throat");
        request.setPrescriptions(new ArrayList<>());

        when(medicalRecordRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenReturn(mockRecord);

        MedicalRecordResponse response = medicalRecordService.createRecord(10L, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Common Flu", response.getDiagnosis());
        verify(medicalRecordRepository, times(1)).findByAppointmentId(10L);
        verify(medicalRecordRepository, times(1)).save(any(MedicalRecord.class));
    }

    @Test
    void testCreateRecordAlreadyExists() {
        CreateMedicalRecordRequest request = new CreateMedicalRecordRequest();
        request.setPatientId(100L);

        when(medicalRecordRepository.findByAppointmentId(10L)).thenReturn(Optional.of(mockRecord));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> medicalRecordService.createRecord(10L, request));
        assertEquals("Medical record already exists for this appointment", exception.getMessage());
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
    }

    @Test
    void testGetPatientHistory() {
        when(medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(mockRecord));

        List<MedicalRecordResponse> history = medicalRecordService.getPatientHistory(100L);

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("Common Flu", history.get(0).getDiagnosis());
        verify(medicalRecordRepository, times(1)).findByPatientIdOrderByCreatedAtDesc(100L);
    }

    @Test
    void testGetRecordDetails() {
        when(medicalRecordRepository.findById(1L)).thenReturn(Optional.of(mockRecord));

        MedicalRecordResponse response = medicalRecordService.getRecord(1L);

        assertNotNull(response);
        assertEquals(10L, response.getAppointmentId());
        assertEquals("Common Flu", response.getDiagnosis());
        verify(medicalRecordRepository, times(1)).findById(1L);
    }
}
