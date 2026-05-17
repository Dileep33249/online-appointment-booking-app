package com.app.admin.service;

import com.app.admin.dto.AdminOverviewResponse;
import com.app.admin.dto.ProviderVerificationRequest;
import com.app.admin.dto.ReviewReportRequest;
import com.app.admin.entity.ProviderVerificationTask;
import com.app.admin.entity.ReviewReport;
import com.app.admin.repository.ProviderVerificationTaskRepository;
import com.app.admin.repository.ReviewReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ProviderVerificationTaskRepository providerVerificationTaskRepository;

    @Mock
    private ReviewReportRepository reviewReportRepository;

    @InjectMocks
    private AdminService adminService;

    private ProviderVerificationTask mockTask;
    private ReviewReport mockReport;

    @BeforeEach
    void setUp() {
        mockTask = ProviderVerificationTask.builder()
                .id(1L)
                .providerId(10L)
                .providerName("Dr. Jane Doe")
                .remarks("Verification Request")
                .status("PENDING")
                .build();

        mockReport = ReviewReport.builder()
                .id(2L)
                .providerId(10L)
                .reportedBy(100L)
                .reason("Spam comment")
                .status("REPORTED")
                .build();
    }

    @Test
    void testCreateVerificationTask() {
        ProviderVerificationRequest request = new ProviderVerificationRequest();
        request.setProviderId(10L);
        request.setProviderName("Dr. Jane Doe");
        request.setRemarks("Verification Request");

        when(providerVerificationTaskRepository.save(any(ProviderVerificationTask.class))).thenReturn(mockTask);

        ProviderVerificationTask created = adminService.createVerificationTask(request);

        assertNotNull(created);
        assertEquals(1L, created.getId());
        assertEquals("PENDING", created.getStatus());
        assertEquals("Dr. Jane Doe", created.getProviderName());
        verify(providerVerificationTaskRepository, times(1)).save(any(ProviderVerificationTask.class));
    }

    @Test
    void testGetPendingVerifications() {
        when(providerVerificationTaskRepository.findByStatus("PENDING")).thenReturn(List.of(mockTask));

        List<ProviderVerificationTask> pending = adminService.getPendingVerifications();

        assertNotNull(pending);
        assertEquals(1, pending.size());
        assertEquals("Dr. Jane Doe", pending.get(0).getProviderName());
        verify(providerVerificationTaskRepository, times(1)).findByStatus("PENDING");
    }

    @Test
    void testApproveProvider() {
        when(providerVerificationTaskRepository.findById(1L)).thenReturn(Optional.of(mockTask));
        when(providerVerificationTaskRepository.save(any(ProviderVerificationTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProviderVerificationTask approved = adminService.approveProvider(1L);

        assertNotNull(approved);
        assertEquals("APPROVED", approved.getStatus());
        verify(providerVerificationTaskRepository, times(1)).findById(1L);
        verify(providerVerificationTaskRepository, times(1)).save(any(ProviderVerificationTask.class));
    }

    @Test
    void testGetOverview() {
        when(providerVerificationTaskRepository.countByStatus("PENDING")).thenReturn(5L);
        when(providerVerificationTaskRepository.countByStatus("APPROVED")).thenReturn(12L);
        when(providerVerificationTaskRepository.countByStatus("REJECTED")).thenReturn(2L);
        when(reviewReportRepository.countByStatus("REPORTED")).thenReturn(3L);

        AdminOverviewResponse overview = adminService.getOverview();

        assertNotNull(overview);
        assertEquals(5, overview.getPendingProviderVerifications());
        assertEquals(12, overview.getApprovedProviders());
        assertEquals(2, overview.getRejectedProviders());
        assertEquals(3, overview.getReportedReviews());
    }
}
