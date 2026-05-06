package com.app.admin.repository;

import com.app.admin.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    List<ReviewReport> findByStatus(String status);
    long countByStatus(String status);
}
