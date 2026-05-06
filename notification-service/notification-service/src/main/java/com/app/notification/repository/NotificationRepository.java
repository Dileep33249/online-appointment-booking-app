package com.app.notification.repository;

import com.app.notification.entity.Notification;
import com.app.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByStatusAndScheduledForBefore(NotificationStatus status, LocalDateTime time);
}
