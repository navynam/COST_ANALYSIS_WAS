package com.costanalysis.domain.comparison.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class ComparisonCreateRequest {

    @NotBlank
    private String title;

    @NotEmpty
    @Size(min = 2, max = 5, message = "비교는 2~5개 견적서만 가능합니다.")
    private List<Long> quotationIds;
}
