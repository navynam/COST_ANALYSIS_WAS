package com.costanalysis.domain.comparison.dto;

import com.costanalysis.domain.comparison.entity.ComparisonSession;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter @Builder
public class ComparisonResponse {
    private Long   id;
    private String title;
    private String quotationIds;
    private String status;
    private String resultJson;
    private String createdByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime analyzedAt;

    public static ComparisonResponse from(ComparisonSession s) {
        return ComparisonResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .quotationIds(s.getQuotationIds())
                .status(s.getStatus())
                .resultJson(s.getResultJson())
                .createdByName(s.getCreatedBy().getName())
                .createdAt(s.getCreatedAt())
                .analyzedAt(s.getAnalyzedAt())
                .build();
    }
}
