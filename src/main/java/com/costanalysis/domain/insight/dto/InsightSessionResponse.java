package com.costanalysis.domain.insight.dto;

import com.costanalysis.domain.insight.entity.InsightSession;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter @Builder
public class InsightSessionResponse {
    private Long   id;
    private String title;
    private Long   quotationId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static InsightSessionResponse from(InsightSession s) {
        return InsightSessionResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .quotationId(s.getQuotation() != null ? s.getQuotation().getId() : null)
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
