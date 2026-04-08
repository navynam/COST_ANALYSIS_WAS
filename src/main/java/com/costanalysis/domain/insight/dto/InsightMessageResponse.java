package com.costanalysis.domain.insight.dto;

import com.costanalysis.domain.insight.entity.InsightMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter @Builder
public class InsightMessageResponse {
    private Long   id;
    private String role;
    private String content;
    private String thinking;
    private OffsetDateTime createdAt;

    public static InsightMessageResponse from(InsightMessage m) {
        return InsightMessageResponse.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .thinking(m.getThinking())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
