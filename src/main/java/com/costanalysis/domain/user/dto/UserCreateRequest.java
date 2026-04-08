package com.costanalysis.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserCreateRequest {

    @NotBlank(message = "사번을 입력하세요.")
    @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "사번은 영문/숫자 4~20자입니다.")
    private String employeeId;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "이름을 입력하세요.")
    private String name;

    private String department;
    private String phone;

    @Pattern(regexp = "USER|ADMIN", message = "역할은 USER 또는 ADMIN 이어야 합니다.")
    private String role = "USER";
}
