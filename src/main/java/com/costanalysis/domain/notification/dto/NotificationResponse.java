package com.costanalysis.domain.notification.dto;

import com.costanalysis.domain.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter @Builder
public class NotificationResponse {
    private Long   id;
    private String type;
    private String title;
    private String body;
    private Long   refId;
    private boolean readFlag;
    private OffsetDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .refId(n.getRefId())
                .readFlag(n.isReadFlag())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
