package com.costanalysis.domain.user.dto;

import com.costanalysis.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter @Builder
public class UserProfileResponse {
    private Long   userId;
    private String employeeId;
    private String name;
    private String department;
    private String phone;
    private String role;
    private String language;
    private boolean notifyEmail;
    private boolean notifyInApp;
    private OffsetDateTime createdAt;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .userId(user.getId())
                .employeeId(user.getEmployeeId())
                .name(user.getName())
                .department(user.getDepartment())
                .phone(user.getPhone())
                .role(user.getRole())
                .language(user.getLanguage())
                .notifyEmail(user.isNotifyEmail())
                .notifyInApp(user.isNotifyInApp())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
