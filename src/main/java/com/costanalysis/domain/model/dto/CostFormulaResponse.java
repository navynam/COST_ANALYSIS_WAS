package com.costanalysis.domain.model.dto;

import com.costanalysis.domain.model.entity.CostFormula;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter @Builder
public class CostFormulaResponse {
    private Long   id;
    private String name;
    private String category;
    private String formula;
    private String description;
    private boolean systemFormula;
    private boolean active;
    private List<String> departments;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CostFormulaResponse from(CostFormula f) {
        return CostFormulaResponse.builder()
                .id(f.getId())
                .name(f.getName())
                .category(f.getCategory())
                .formula(f.getFormula())
                .description(f.getDescription())
                .systemFormula(f.isSystemFormula())
                .active(f.isActive())
                .departments(f.getDepartments())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
    }
}
