package com.costanalysis.domain.verification.service;

import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.entity.Quotation;
import com.costanalysis.domain.quotation.repository.ParsedItemRepository;
import com.costanalysis.domain.quotation.repository.QuotationRepository;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.domain.user.repository.UserRepository;
import com.costanalysis.domain.verification.dto.VerificationDecisionRequest;
import com.costanalysis.domain.verification.dto.VerificationResponse;
import com.costanalysis.domain.verification.entity.VerificationResult;
import com.costanalysis.domain.verification.repository.VerificationRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@org.springframework.context.annotation.Profile("!mock")
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final QuotationRepository    quotationRepository;
    private final ParsedItemRepository   parsedItemRepository;
    private final UserRepository         userRepository;

    /**
     * 자동 검증 결과 생성 (파싱 직후 또는 요청 시)
     */
    @Transactional
    public VerificationResponse createVerification(Long quotationId) {
        Quotation q = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTATION_NOT_FOUND));

        List<ParsedItem> items = parsedItemRepository.findByQuotation_IdOrderByRowIndex(quotationId);

        BigDecimal avgConf = items.stream()
                .filter(i -> i.getConfidenceScore() != null)
                .map(ParsedItem::getConfidenceScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(items.size(), 1)), 4, RoundingMode.HALF_UP);

        long lowConf = items.stream()
                .filter(i -> i.getConfidenceScore() != null && i.getConfidenceScore().compareTo(BigDecimal.valueOf(0.7)) < 0)
                .count();

        String issues = buildIssuesJson(items);

        VerificationResult vr = VerificationResult.builder()
                .quotation(q)
                .status("PENDING")
                .overallConfidence(avgConf)
                .totalItems(items.size())
                .approvedItems(0)
                .rejectedItems(0)
                .summary("총 " + items.size() + "개 항목 검증. 신뢰도 낮은 항목 " + lowConf + "개 주의 필요.")
                .issues(issues)
                .build();

        return VerificationResponse.from(verificationRepository.save(vr));
    }

    public VerificationResponse getLatest(Long quotationId) {
        return verificationRepository.findTopByQuotation_IdOrderByCreatedAtDesc(quotationId)
                .map(VerificationResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));
    }

    @Transactional
    public VerificationResponse decide(Long verificationId, Long userId, VerificationDecisionRequest req) {
        VerificationResult vr = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        vr.setStatus(req.getDecision());
        vr.setVerifierNote(req.getNote());
        vr.setVerifiedBy(user);
        vr.setVerifiedAt(OffsetDateTime.now());

        List<ParsedItem> items = parsedItemRepository.findByQuotation_IdOrderByRowIndex(vr.getQuotation().getId());
        if ("APPROVED".equals(req.getDecision())) {
            vr.setApprovedItems(items.size());
            vr.setRejectedItems(0);
        } else if ("REJECTED".equals(req.getDecision())) {
            vr.setApprovedItems(0);
            vr.setRejectedItems(items.size());
        }

        return VerificationResponse.from(verificationRepository.save(vr));
    }

    private String buildIssuesJson(List<ParsedItem> items) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (ParsedItem item : items) {
            if (item.getConfidenceScore() != null && item.getConfidenceScore().compareTo(BigDecimal.valueOf(0.7)) < 0) {
                if (!first) sb.append(",");
                sb.append("{\"rowIndex\":").append(item.getRowIndex())
                  .append(",\"itemName\":\"").append(item.getItemName()).append("\"")
                  .append(",\"confidence\":").append(item.getConfidenceScore()).append("}");
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
