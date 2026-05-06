package com.app.admin.service;

import com.app.admin.dto.AdminOverviewResponse;
import com.app.admin.dto.ProviderVerificationRequest;
import com.app.admin.dto.ReviewReportRequest;
import com.app.admin.entity.ProviderVerificationTask;
import com.app.admin.entity.ReviewReport;
import com.app.admin.repository.ProviderVerificationTaskRepository;
import com.app.admin.repository.ReviewReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminService {
    private final ProviderVerificationTaskRepository providerVerificationTaskRepository;
    private final ReviewReportRepository reviewReportRepository;

    public AdminService(ProviderVerificationTaskRepository providerVerificationTaskRepository,
                        ReviewReportRepository reviewReportRepository) {
        this.providerVerificationTaskRepository = providerVerificationTaskRepository;
        this.reviewReportRepository = reviewReportRepository;
    }

    public ProviderVerificationTask createVerificationTask(ProviderVerificationRequest request) {
        ProviderVerificationTask task = ProviderVerificationTask.builder()
                .providerId(request.getProviderId())
                .providerName(request.getProviderName())
                .remarks(request.getRemarks())
                .status("PENDING")
                .build();
        return providerVerificationTaskRepository.save(task);
    }

    public List<ProviderVerificationTask> getPendingVerifications() {
        return providerVerificationTaskRepository.findByStatus("PENDING");
    }

    public ProviderVerificationTask approveProvider(Long taskId) {
        ProviderVerificationTask task = loadTask(taskId);
        task.setStatus("APPROVED");
        return providerVerificationTaskRepository.save(task);
    }

    public ProviderVerificationTask rejectProvider(Long taskId, String remarks) {
        ProviderVerificationTask task = loadTask(taskId);
        task.setStatus("REJECTED");
        task.setRemarks(remarks);
        return providerVerificationTaskRepository.save(task);
    }

    public ReviewReport reportReview(ReviewReportRequest request) {
        ReviewReport report = ReviewReport.builder()
                .providerId(request.getProviderId())
                .reportedBy(request.getReportedBy())
                .reason(request.getReason())
                .status("REPORTED")
                .build();
        return reviewReportRepository.save(report);
    }

    public List<ReviewReport> getReportedReviews() {
        return reviewReportRepository.findByStatus("REPORTED");
    }

    public AdminOverviewResponse getOverview() {
        return AdminOverviewResponse.builder()
                .pendingProviderVerifications(providerVerificationTaskRepository.countByStatus("PENDING"))
                .approvedProviders(providerVerificationTaskRepository.countByStatus("APPROVED"))
                .rejectedProviders(providerVerificationTaskRepository.countByStatus("REJECTED"))
                .reportedReviews(reviewReportRepository.countByStatus("REPORTED"))
                .build();
    }

    private ProviderVerificationTask loadTask(Long taskId) {
        return providerVerificationTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Verification task not found: " + taskId));
    }
}
