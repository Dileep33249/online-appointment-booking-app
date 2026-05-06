package com.app.admin.controller;

import com.app.admin.dto.AdminOverviewResponse;
import com.app.admin.dto.ProviderVerificationRequest;
import com.app.admin.dto.ReviewReportRequest;
import com.app.admin.entity.ProviderVerificationTask;
import com.app.admin.entity.ReviewReport;
import com.app.admin.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/providers/verification-task")
    public ResponseEntity<ProviderVerificationTask> createVerificationTask(@Valid @RequestBody ProviderVerificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createVerificationTask(request));
    }

    @GetMapping("/providers/pending-verification")
    public ResponseEntity<List<ProviderVerificationTask>> getPendingVerifications() {
        return ResponseEntity.ok(adminService.getPendingVerifications());
    }

    @PutMapping("/providers/{taskId}/approve")
    public ResponseEntity<ProviderVerificationTask> approveProvider(@PathVariable Long taskId) {
        return ResponseEntity.ok(adminService.approveProvider(taskId));
    }

    @PutMapping("/providers/{taskId}/reject")
    public ResponseEntity<ProviderVerificationTask> rejectProvider(@PathVariable Long taskId,
                                                                   @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(adminService.rejectProvider(taskId, remarks));
    }

    @PostMapping("/reviews/report")
    public ResponseEntity<ReviewReport> reportReview(@Valid @RequestBody ReviewReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.reportReview(request));
    }

    @GetMapping("/reviews/reported")
    public ResponseEntity<List<ReviewReport>> getReportedReviews() {
        return ResponseEntity.ok(adminService.getReportedReviews());
    }

    @GetMapping("/analytics/overview")
    public ResponseEntity<AdminOverviewResponse> getOverview() {
        return ResponseEntity.ok(adminService.getOverview());
    }
}
