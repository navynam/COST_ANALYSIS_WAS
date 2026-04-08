package com.costanalysis.domain.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "change_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChangeRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "formula_id", nullable = false)
    private Long formulaId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "requester_name", nullable = false, length = 50)
    private String requesterName;

    @Column(length = 100)
    private String department;

    @Column(name = "task_name", length = 200)
    private String taskName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "original_formula", columnDefinition = "jsonb")
    private String originalFormula;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modified_fields", columnDefinition = "jsonb")
    private String modifiedFields;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";  // PENDING | APPROVED | REJECTED

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reviewer_comment", columnDefinition = "TEXT")
    private String reviewerComment;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "approved_departments", columnDefinition = "jsonb")
    private List<String> approvedDepartments;
}
