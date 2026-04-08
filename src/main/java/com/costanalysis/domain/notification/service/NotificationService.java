package com.costanalysis.domain.notification.service;

import com.costanalysis.domain.notification.dto.NotificationResponse;
import com.costanalysis.domain.notification.entity.ActivityLog;
import com.costanalysis.domain.notification.entity.Notification;
import com.costanalysis.domain.notification.repository.ActivityLogRepository;
import com.costanalysis.domain.notification.repository.NotificationRepository;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.domain.user.repository.UserRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@org.springframework.context.annotation.Profile("!mock")
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository  activityLogRepository;
    private final UserRepository         userRepository;

    public Page<NotificationResponse> list(Long userId, Pageable pageable) {
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::from);
    }

    public Map<String, Long> unreadCount(Long userId) {
        return Map.of("count", notificationRepository.countByUser_IdAndReadFlagFalse(userId));
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    public void markRead(Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST));
        n.setReadFlag(true);
        n.setReadAt(OffsetDateTime.now());
        notificationRepository.save(n);
    }

    // ── 내부 전송 helper (다른 서비스에서 호출) ───────────────────────────────

    @Transactional
    public void send(Long userId, String type, String title, String body, Long refId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isNotifyInApp()) return;

        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .refId(refId)
                .build();
        notificationRepository.save(n);
    }

    @Transactional
    public void log(Long userId, String action, String target, Long targetId, String ip) {
        User user = userRepository.findById(userId).orElse(null);
        ActivityLog log = ActivityLog.builder()
                .user(user)
                .action(action)
                .target(target)
                .targetId(targetId)
                .ipAddress(ip)
                .build();
        activityLogRepository.save(log);
    }
}
