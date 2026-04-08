package com.costanalysis.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private long   accessExpiresIn;  // 초
    private long   userId;
    private String name;
    private String role;
    private String department;
}
