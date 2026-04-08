package com.costanalysis.domain.verification.entity;

import com.costanalysis.domain.quotation.entity.Quotation;
import com.costanalysis.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "verification_results")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class VerificationResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";   // PENDING | APPROVED | REJECTED | PARTIAL

    @Column(precision = 5, scale = 2)
    private BigDecimal overallConfidence;

    @Column(columnDefinition = "TEXT")
    private String summary;         // AI 검토 요약

    @Column(columnDefinition = "TEXT")
    private String issues;          // JSON 배열: 이슈 항목들

    @Column(columnDefinition = "TEXT")
    private String verifierNote;    // 검토자 메모

    private Integer totalItems;
    private Integer approvedItems;
    private Integer rejectedItems;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    private OffsetDateTime verifiedAt;
}
