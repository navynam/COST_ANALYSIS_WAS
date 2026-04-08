package com.costanalysis.domain.comparison.service;

import com.costanalysis.domain.comparison.dto.ComparisonCreateRequest;
import com.costanalysis.domain.comparison.dto.ComparisonResponse;
import com.costanalysis.domain.comparison.entity.ComparisonSession;
import com.costanalysis.domain.comparison.repository.ComparisonRepository;
import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.entity.Quotation;
import com.costanalysis.domain.quotation.repository.ParsedItemRepository;
import com.costanalysis.domain.quotation.repository.QuotationRepository;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.domain.user.repository.UserRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@org.springframework.context.annotation.Profile("!mock")
public class ComparisonService {

    private final ComparisonRepository  comparisonRepository;
    private final QuotationRepository   quotationRepository;
    private final ParsedItemRepository  parsedItemRepository;
    private final UserRepository        userRepository;

    @Transactional
    public ComparisonResponse create(Long userId, ComparisonCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String idsJson = "[" + req.getQuotationIds().stream()
                .map(String::valueOf).collect(Collectors.joining(",")) + "]";

        ComparisonSession session = ComparisonSession.builder()
                .createdBy(user)
                .title(req.getTitle())
                .quotationIds(idsJson)
                .build();

        // 간단한 비교 분석 수행
        String result = buildComparisonResult(req.getQuotationIds());
        session.setResultJson(result);
        session.setStatus("DONE");
        session.setAnalyzedAt(OffsetDateTime.now());

        return ComparisonResponse.from(comparisonRepository.save(session));
    }

    public Page<ComparisonResponse> listMine(Long userId, Pageable pageable) {
        return comparisonRepository
                .findByCreatedBy_IdOrderByCreatedAtDesc(userId, pageable)
                .map(ComparisonResponse::from);
    }

    public ComparisonResponse getDetail(Long id) {
        return comparisonRepository.findById(id)
                .map(ComparisonResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPARISON_NOT_FOUND));
    }

    @Transactional
    public void delete(Long id) {
        ComparisonSession s = comparisonRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMPARISON_NOT_FOUND));
        comparisonRepository.delete(s);
    }

    // ── private ──────────────────────────────────────────────────────────────

    private String buildComparisonResult(List<Long> quotationIds) {
        StringBuilder sb = new StringBuilder("{\"quotations\":[");
        boolean first = true;
        for (Long qId : quotationIds) {
            Optional<Quotation> opt = quotationRepository.findById(qId);
            if (opt.isEmpty()) continue;
            Quotation q = opt.get();
            List<ParsedItem> items = parsedItemRepository.findByQuotation_IdOrderByRowIndex(qId);
            BigDecimal total = items.stream()
                    .filter(i -> i.getTotalPrice() != null)
                    .map(ParsedItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!first) sb.append(",");
            sb.append("{\"id\":").append(qId)
              .append(",\"title\":\"").append(q.getOriginalFilename()).append("\"")
              .append(",\"vendor\":\"").append(q.getVendor() != null ? q.getVendor() : "").append("\"")
              .append(",\"totalItems\":").append(items.size())
              .append(",\"totalAmount\":").append(total.setScale(0, RoundingMode.HALF_UP))
              .append("}");
            first = false;
        }
        sb.append("]}");
        return sb.toString();
    }
}
