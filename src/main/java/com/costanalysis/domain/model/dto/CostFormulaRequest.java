package com.costanalysis.domain.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class CostFormulaRequest {

    @NotBlank(message = "수식 이름을 입력하세요.")
    private String name;

    @NotBlank(message = "카테고리를 입력하세요.")
    private String category;

    @NotBlank(message = "수식을 입력하세요.")
    private String formula;

    private String description;
    private Boolean active;
    private List<String> departments;
}
