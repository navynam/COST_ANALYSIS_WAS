package com.costanalysis.domain.insight.dto;

import lombok.Getter;

@Getter
public class SessionCreateRequest {
    private Long   quotationId;   // optional: 특정 견적서 컨텍스트
    private String title;
}
