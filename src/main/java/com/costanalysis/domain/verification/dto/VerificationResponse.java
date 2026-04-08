package com.costanalysis.domain.verification.dto;

import com.costanalysis.domain.verification.entity.VerificationResult;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Builder
public class VerificationResponse {
    private Long   id;
    private Long   quotationId;
    private String status;
    private BigDecimal overallConfidence;
    private String summary;
    private String issues;
    private String verifierNote;
    private Integer totalItems;
    private Integer approvedItems;
    private Integer rejectedItems;
    private OffsetDateTime createdAt;
    private OffsetDateTime verifiedAt;

    public static VerificationResponse from(VerificationResult v) {
        return VerificationResponse.builder()
                .id(v.getId())
                .quotationId(v.getQuotation().getId())
                .status(v.getStatus())
                .overallConfidence(v.getOverallConfidence())
                .summary(v.getSummary())
                .issues(v.getIssues())
                .verifierNote(v.getVerifierNote())
                .totalItems(v.getTotalItems())
                .approvedItems(v.getApprovedItems())
                .rejectedItems(v.getRejectedItems())
                .createdAt(v.getCreatedAt())
                .verifiedAt(v.getVerifiedAt())
                .build();
    }
}
