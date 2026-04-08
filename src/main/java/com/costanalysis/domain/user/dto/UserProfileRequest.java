package com.costanalysis.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UserProfileRequest {

    @NotBlank(message = "이름을 입력하세요.")
    private String name;

    private String department;
    private String phone;
    private String language;
    private Boolean notifyEmail;
    private Boolean notifyInApp;
}
