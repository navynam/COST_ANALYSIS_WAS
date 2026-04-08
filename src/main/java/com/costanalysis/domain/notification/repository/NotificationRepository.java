package com.costanalysis.domain.notification.repository;

import com.costanalysis.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUser_IdAndReadFlagFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readFlag = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.readFlag = false")
    void markAllReadByUserId(@Param("userId") Long userId);
}
