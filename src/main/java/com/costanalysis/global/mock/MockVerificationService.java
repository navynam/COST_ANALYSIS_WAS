package com.costanalysis.global.mock;

import com.costanalysis.domain.verification.dto.VerificationDecisionRequest;
import com.costanalysis.domain.verification.dto.VerificationResponse;
import com.costanalysis.domain.verification.service.VerificationService;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock 프로파일 전용 검증 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockVerificationService extends VerificationService {

    private final MockDataStore dataStore;
    private final ConcurrentHashMap<Long, VerificationResponse> verifications = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(0);

    public MockVerificationService(MockDataStore dataStore) {
        super(null, null, null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public VerificationResponse createVerification(Long quotationId) {
        Map<String, Object> q = dataStore.getQuotations().get(quotationId);
        if (q == null) throw new BusinessException(ErrorCode.QUOTATION_NOT_FOUND);

        Integer parsedItems = (Integer) q.get("parsedItems");
        int totalItems = parsedItems != null ? parsedItems : 10;

        Long id = seq.incrementAndGet();
        VerificationResponse vr = VerificationResponse.builder()
                .id(id)
                .quotationId(quotationId)
                .status("PENDING")
                .overallConfidence(BigDecimal.valueOf(0.92))
                .totalItems(totalItems)
                .approvedItems(0)
                .rejectedItems(0)
                .summary("총 " + totalItems + "개 항목 검증 대기. 신뢰도 양호.")
                .issues("[]")
                .createdAt(OffsetDateTime.now())
                .build();
        verifications.put(id, vr);
        log.debug("[MOCK] 검증 생성: id={}, quotationId={}", id, quotationId);
        return vr;
    }

    @Override
    public VerificationResponse getLatest(Long quotationId) {
        return verifications.values().stream()
                .filter(v -> quotationId.equals(v.getQuotationId()))
                .reduce((a, b) -> b) // 마지막 것
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    @Override
    public VerificationResponse decide(Long verificationId, Long userId, VerificationDecisionRequest req) {
        VerificationResponse existing = verifications.get(verificationId);
        if (existing == null) throw new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND);

        VerificationResponse updated = VerificationResponse.builder()
                .id(existing.getId())
                .quotationId(existing.getQuotationId())
                .status(req.getDecision())
                .overallConfidence(existing.getOverallConfidence())
                .totalItems(existing.getTotalItems())
                .approvedItems("APPROVED".equals(req.getDecision()) ? existing.getTotalItems() : 0)
                .rejectedItems("REJECTED".equals(req.getDecision()) ? existing.getTotalItems() : 0)
                .summary(existing.getSummary())
                .issues(existing.getIssues())
                .verifierNote(req.getNote())
                .createdAt(existing.getCreatedAt())
                .verifiedAt(OffsetDateTime.now())
                .build();
        verifications.put(verificationId, updated);
        log.debug("[MOCK] 검증 결정: id={}, decision={}", verificationId, req.getDecision());
        return updated;
    }
}
