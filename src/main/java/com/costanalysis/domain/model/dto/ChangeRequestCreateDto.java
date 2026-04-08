package com.costanalysis.domain.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ChangeRequestCreateDto {

    @NotNull(message = "수식 ID를 입력하세요.")
    private Long formulaId;

    @NotBlank(message = "업무명을 입력하세요.")
    private String taskName;

    /** 원본 수식 스냅샷 (JSON 문자열) */
    private String originalFormula;

    /** 변경된 필드들 (JSON 문자열) */
    @NotBlank(message = "변경 내용을 입력하세요.")
    private String modifiedFields;

    @NotBlank(message = "변경 사유를 입력하세요.")
    private String reason;
}
