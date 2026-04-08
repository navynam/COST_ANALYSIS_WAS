package com.costanalysis.domain.notification.entity;

import com.costanalysis.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "activity_logs")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 50)
    private String action;   // LOGIN | UPLOAD | PARSE | VERIFY | etc.

    @Column(length = 100)
    private String target;   // 대상 리소스 설명

    private Long   targetId;

    @Column(length = 45)
    private String ipAddress;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
