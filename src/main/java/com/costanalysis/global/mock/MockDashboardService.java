package com.costanalysis.global.mock;

import com.costanalysis.domain.dashboard.dto.DashboardStats;
import com.costanalysis.domain.dashboard.dto.DashboardStats.RecentQuotation;
import com.costanalysis.domain.dashboard.dto.DashboardStats.StatusCount;
import com.costanalysis.domain.dashboard.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock 프로파일 전용 대시보드 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockDashboardService extends DashboardService {

    private final MockDataStore dataStore;

    public MockDashboardService(MockDataStore dataStore) {
        super(null, null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public DashboardStats getStats() {
        Collection<Map<String, Object>> quotations = dataStore.getQuotations().values();

        // 상태별 집계
        Map<String, Long> statusMap = quotations.stream()
                .collect(Collectors.groupingBy(q -> (String) q.get("status"), Collectors.counting()));

        List<StatusCount> statusBreakdown = statusMap.entrySet().stream()
                .map(e -> StatusCount.builder().status(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());

        long parsedToday = quotations.stream()
                .filter(q -> Set.of("verifying", "verified", "analyzing", "analyzed").contains(q.get("status")))
                .count();

        long pendingVerification = quotations.stream()
                .filter(q -> "verifying".equals(q.get("status")))
                .count();

        // 최근 10건
        List<RecentQuotation> recent = quotations.stream()
                .sorted(Comparator.comparing((Map<String, Object> q) -> (OffsetDateTime) q.get("createdAt")).reversed())
                .limit(10)
                .map(q -> RecentQuotation.builder()
                        .id((Long) q.get("id"))
                        .filename((String) q.get("name"))
                        .status((String) q.get("status"))
                        .uploaderName((String) q.get("uploader"))
                        .createdAt(q.get("createdAt").toString())
                        .build())
                .collect(Collectors.toList());

        return DashboardStats.builder()
                .totalQuotations(quotations.size())
                .parsedToday(parsedToday)
                .pendingVerification(pendingVerification)
                .totalUsers(dataStore.getUsers().size())
                .totalAmountThisMonth(BigDecimal.valueOf(1_250_000_000L))
                .recentQuotations(recent)
                .statusBreakdown(statusBreakdown)
                .build();
    }
}
