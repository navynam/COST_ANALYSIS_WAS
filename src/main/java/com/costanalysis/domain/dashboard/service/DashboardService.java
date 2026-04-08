package com.costanalysis.domain.dashboard.service;

import com.costanalysis.domain.dashboard.dto.DashboardStats;
import com.costanalysis.domain.dashboard.dto.DashboardStats.RecentQuotation;
import com.costanalysis.domain.dashboard.dto.DashboardStats.StatusCount;
import com.costanalysis.domain.quotation.entity.Quotation;
import com.costanalysis.domain.quotation.repository.QuotationRepository;
import com.costanalysis.domain.user.repository.UserRepository;
import com.costanalysis.domain.verification.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!mock")
public class DashboardService {

    private final QuotationRepository    quotationRepository;
    private final UserRepository         userRepository;
    private final VerificationRepository verificationRepository;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DashboardStats getStats() {
        List<Quotation> all = quotationRepository.findAll();

        // 오늘 파싱된 건수
        OffsetDateTime todayStart = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);
        long parsedToday = all.stream()
                .filter(q -> "PARSED".equals(q.getStatus()) && q.getUpdatedAt().isAfter(todayStart))
                .count();

        // 검증 대기 (PENDING 상태)
        long pendingVerification = verificationRepository.findAll().stream()
                .filter(v -> "PENDING".equals(v.getStatus()))
                .count();

        // 이번 달 총 금액 (PARSED 상태)
        OffsetDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        BigDecimal totalAmount = quotationRepository.findAll().stream()
                .filter(q -> "PARSED".equals(q.getStatus()) && q.getCreatedAt().isAfter(monthStart))
                .flatMap(q -> q.getParsedItems().stream())
                .filter(i -> i.getTotalPrice() != null)
                .map(i -> i.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 상태별 집계
        Map<String, Long> statusMap = all.stream()
                .collect(Collectors.groupingBy(Quotation::getStatus, Collectors.counting()));
        List<StatusCount> statusBreakdown = statusMap.entrySet().stream()
                .map(e -> StatusCount.builder().status(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());

        // 최근 10건
        List<RecentQuotation> recent = quotationRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .stream()
                .map(q -> RecentQuotation.builder()
                        .id(q.getId())
                        .filename(q.getOriginalFilename())
                        .status(q.getStatus())
                        .uploaderName(q.getUploadedBy().getName())
                        .createdAt(q.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        return DashboardStats.builder()
                .totalQuotations(all.size())
                .parsedToday(parsedToday)
                .pendingVerification(pendingVerification)
                .totalUsers(userRepository.count())
                .totalAmountThisMonth(totalAmount)
                .recentQuotations(recent)
                .statusBreakdown(statusBreakdown)
                .build();
    }
}
