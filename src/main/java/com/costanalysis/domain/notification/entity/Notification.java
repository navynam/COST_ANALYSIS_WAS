package com.costanalysis.domain.notification.entity;

import com.costanalysis.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String type;    // PARSING_DONE | VERIFICATION_REQUIRED | COMPARISON_DONE | SYSTEM

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    private Long   refId;   // 관련 리소스 ID (quotationId 등)

    @Column(nullable = false)
    @Builder.Default
    private boolean readFlag = false;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    private OffsetDateTime readAt;
}
