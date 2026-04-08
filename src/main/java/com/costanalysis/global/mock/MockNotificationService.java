package com.costanalysis.global.mock;

import com.costanalysis.domain.notification.dto.NotificationResponse;
import com.costanalysis.domain.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock 프로파일 전용 알림 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockNotificationService extends NotificationService {

    private final MockDataStore dataStore;

    public MockNotificationService(MockDataStore dataStore) {
        super(null, null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public Page<NotificationResponse> list(Long userId, Pageable pageable) {
        List<NotificationResponse> filtered = dataStore.getNotifications().values().stream()
                .filter(n -> userId.equals(n.get("userId")))
                .sorted(Comparator.comparing((Map<String, Object> n) -> (OffsetDateTime) n.get("createdAt")).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<NotificationResponse> sub = start >= filtered.size()
                ? Collections.emptyList() : filtered.subList(start, end);
        return new PageImpl<>(sub, pageable, filtered.size());
    }

    @Override
    public Map<String, Long> unreadCount(Long userId) {
        long count = dataStore.getNotifications().values().stream()
                .filter(n -> userId.equals(n.get("userId")) && !(boolean) n.get("readFlag"))
                .count();
        return Map.of("count", count);
    }

    @Override
    public void markAllRead(Long userId) {
        dataStore.getNotifications().values().stream()
                .filter(n -> userId.equals(n.get("userId")))
                .forEach(n -> {
                    n.put("readFlag", true);
                    n.put("readAt", OffsetDateTime.now());
                });
        log.debug("[MOCK] 전체 읽음 처리: userId={}", userId);
    }

    @Override
    public void markRead(Long notificationId) {
        Map<String, Object> n = dataStore.getNotifications().get(notificationId);
        if (n != null) {
            n.put("readFlag", true);
            n.put("readAt", OffsetDateTime.now());
        }
        log.debug("[MOCK] 읽음 처리: notificationId={}", notificationId);
    }

    @Override
    public void send(Long userId, String type, String title, String body, Long refId) {
        Long id = dataStore.nextNotificationId();
        Map<String, Object> n = new java.util.concurrent.ConcurrentHashMap<>();
        n.put("id", id);
        n.put("userId", userId);
        n.put("type", type);
        n.put("title", title);
        n.put("body", body);
        n.put("refId", refId);
        n.put("readFlag", false);
        n.put("createdAt", OffsetDateTime.now());
        dataStore.getNotifications().put(id, n);
        log.debug("[MOCK] 알림 전송: userId={}, title={}", userId, title);
    }

    @Override
    public void log(Long userId, String action, String target, Long targetId, String ip) {
        log.debug("[MOCK] 활동 로그: userId={}, action={}, target={}", userId, action, target);
    }

    private NotificationResponse toResponse(Map<String, Object> n) {
        return NotificationResponse.builder()
                .id((Long) n.get("id"))
                .type((String) n.get("type"))
                .title((String) n.get("title"))
                .body((String) n.get("body"))
                .refId((Long) n.get("refId"))
                .readFlag((boolean) n.get("readFlag"))
                .createdAt((OffsetDateTime) n.get("createdAt"))
                .build();
    }
}
