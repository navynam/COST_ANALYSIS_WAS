package com.costanalysis.domain.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class VerificationDecisionRequest {

    @NotBlank
    @Pattern(regexp = "APPROVED|REJECTED|PARTIAL")
    private String decision;

    private String note;
}
