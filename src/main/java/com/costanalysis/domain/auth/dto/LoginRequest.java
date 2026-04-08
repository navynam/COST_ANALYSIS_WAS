package com.costanalysis.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {

    @NotBlank(message = "사번을 입력하세요.")
    private String employeeId;

    @NotBlank(message = "비밀번호를 입력하세요.")
    private String password;
}
