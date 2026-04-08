package com.costanalysis.domain.insight.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChatRequest {

    @NotBlank(message = "메시지를 입력하세요.")
    private String message;

    private boolean useThinking = false;
}
