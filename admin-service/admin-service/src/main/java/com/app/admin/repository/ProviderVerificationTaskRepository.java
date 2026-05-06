package com.app.admin.repository;

import com.app.admin.entity.ProviderVerificationTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderVerificationTaskRepository extends JpaRepository<ProviderVerificationTask, Long> {
    List<ProviderVerificationTask> findByStatus(String status);
    long countByStatus(String status);
}
