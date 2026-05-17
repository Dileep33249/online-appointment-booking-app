package com.app.payment.repository;

import com.app.payment.entity.Payment;
import com.app.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findTopByAppointmentIdOrderByUpdatedAtDescIdDesc(Long appointmentId);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByProviderId(Long providerId);
    List<Payment> findByStatus(PaymentStatus status);
    Optional<Payment> findByTransactionId(String transactionId);
}
