package com.costanalysis.domain.analysis.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Builder
public class AnalysisResult {
    private Long   quotationId;
    private int    totalItems;
    private BigDecimal totalAmount;
    private List<CategoryGroup> categoryGroups;
    private List<AnomalyItem>   anomalies;

    @Getter @Builder
    public static class CategoryGroup {
        private String     category;
        private int        itemCount;
        private BigDecimal totalAmount;
        private BigDecimal ratio;   // 전체 대비 비율
    }

    @Getter @Builder
    public static class AnomalyItem {
        private Long       itemId;
        private String     itemName;
        private BigDecimal unitPrice;
        private BigDecimal avgUnitPrice;
        private BigDecimal deviation;   // (unitPrice - avg) / avg
        private String     reason;
    }
}
