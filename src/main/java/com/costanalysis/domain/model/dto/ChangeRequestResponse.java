package com.costanalysis.domain.model.dto;

import com.costanalysis.domain.model.entity.ChangeRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter @Builder
public class ChangeRequestResponse {

    private Long id;
    private Long formulaId;
    private Long requesterId;
    private String requesterName;
    private String department;
    private String taskName;
    private String originalFormula;
    private String modifiedFields;
    private String status;
    private String reason;
    private OffsetDateTime createdAt;
    private OffsetDateTime reviewedAt;
    private String reviewerComment;
    private List<String> approvedDepartments;

    public static ChangeRequestResponse from(ChangeRequest cr) {
        return ChangeRequestResponse.builder()
                .id(cr.getId())
                .formulaId(cr.getFormulaId())
                .requesterId(cr.getRequesterId())
                .requesterName(cr.getRequesterName())
                .department(cr.getDepartment())
                .taskName(cr.getTaskName())
                .originalFormula(cr.getOriginalFormula())
                .modifiedFields(cr.getModifiedFields())
                .status(cr.getStatus())
                .reason(cr.getReason())
                .createdAt(cr.getCreatedAt())
                .reviewedAt(cr.getReviewedAt())
                .reviewerComment(cr.getReviewerComment())
                .approvedDepartments(cr.getApprovedDepartments())
                .build();
    }
}
