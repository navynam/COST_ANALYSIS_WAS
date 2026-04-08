package com.costanalysis.domain.quotation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ParsingNoteCreateDto {

    @NotNull(message = "파일 ID를 입력하세요.")
    private Long fileId;

    /** parsing | upload | format | improvement */
    private String type;

    @NotBlank(message = "노트 내용을 입력하세요.")
    private String content;
}
