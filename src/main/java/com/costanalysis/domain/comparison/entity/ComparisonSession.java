package com.costanalysis.domain.comparison.entity;

import com.costanalysis.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "comparison_sessions")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ComparisonSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    // 비교 대상 견적서 ID 목록 (JSON 배열 저장)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String quotationIds;   // "[1,2,3]"

    // AI 생성 비교 요약 (JSON)
    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "CREATED";  // CREATED | ANALYZING | DONE | FAILED

    @CreationTimestamp
    private OffsetDateTime createdAt;

    private OffsetDateTime analyzedAt;
}
