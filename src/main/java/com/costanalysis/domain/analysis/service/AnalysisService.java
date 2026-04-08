package com.costanalysis.domain.analysis.service;

import com.costanalysis.domain.analysis.dto.AnalysisResult;
import com.costanalysis.domain.analysis.dto.AnalysisResult.AnomalyItem;
import com.costanalysis.domain.analysis.dto.AnalysisResult.CategoryGroup;
import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.repository.ParsedItemRepository;
import com.costanalysis.domain.quotation.repository.QuotationRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!mock")
public class AnalysisService {

    private static final BigDecimal ANOMALY_THRESHOLD = BigDecimal.valueOf(0.5); // 50% 이상 편차

    private final ParsedItemRepository parsedItemRepository;
    private final QuotationRepository  quotationRepository;

    public AnalysisResult analyze(Long quotationId) {
        if (!quotationRepository.existsById(quotationId)) {
            throw new BusinessException(ErrorCode.QUOTATION_NOT_FOUND);
        }
        List<ParsedItem> items = parsedItemRepository.findByQuotation_IdOrderByRowIndex(quotationId);

        BigDecimal total = items.stream()
                .filter(i -> i.getTotalPrice() != null)
                .map(ParsedItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 카테고리별 그룹
        Map<String, List<ParsedItem>> byCategory = items.stream()
                .collect(Collectors.groupingBy(i -> i.getCategory() != null ? i.getCategory() : "미분류"));

        List<CategoryGroup> groups = byCategory.entrySet().stream()
                .map(entry -> {
                    BigDecimal catTotal = entry.getValue().stream()
                            .filter(i -> i.getTotalPrice() != null)
                            .map(ParsedItem::getTotalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal ratio = total.compareTo(BigDecimal.ZERO) > 0
                            ? catTotal.divide(total, 4, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    return CategoryGroup.builder()
                            .category(entry.getKey())
                            .itemCount(entry.getValue().size())
                            .totalAmount(catTotal)
                            .ratio(ratio)
                            .build();
                })
                .sorted(Comparator.comparing(CategoryGroup::getTotalAmount).reversed())
                .collect(Collectors.toList());

        // 이상값 탐지: 같은 카테고리 내 단가 편차
        List<AnomalyItem> anomalies = detectAnomalies(items);

        return AnalysisResult.builder()
                .quotationId(quotationId)
                .totalItems(items.size())
                .totalAmount(total)
                .categoryGroups(groups)
                .anomalies(anomalies)
                .build();
    }

    private List<AnomalyItem> detectAnomalies(List<ParsedItem> items) {
        List<AnomalyItem> result = new ArrayList<>();

        Map<String, List<ParsedItem>> byCategory = items.stream()
                .filter(i -> i.getUnitPrice() != null && i.getCategory() != null)
                .collect(Collectors.groupingBy(ParsedItem::getCategory));

        for (var entry : byCategory.entrySet()) {
            List<ParsedItem> group = entry.getValue();
            if (group.size() < 2) continue;

            BigDecimal avg = group.stream()
                    .map(ParsedItem::getUnitPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(group.size()), 4, RoundingMode.HALF_UP);

            if (avg.compareTo(BigDecimal.ZERO) == 0) continue;

            for (ParsedItem item : group) {
                BigDecimal dev = item.getUnitPrice().subtract(avg)
                        .abs()
                        .divide(avg, 4, RoundingMode.HALF_UP);
                if (dev.compareTo(ANOMALY_THRESHOLD) > 0) {
                    result.add(AnomalyItem.builder()
                            .itemId(item.getId())
                            .itemName(item.getItemName())
                            .unitPrice(item.getUnitPrice())
                            .avgUnitPrice(avg)
                            .deviation(dev)
                            .reason("카테고리 평균 단가 대비 " + dev.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "% 편차")
                            .build());
                }
            }
        }
        return result;
    }
}
