package com.costanalysis.domain.insight.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "insight_messages")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class InsightMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private InsightSession session;

    @Column(nullable = false, length = 10)
    private String role;   // user | assistant

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String thinking;   // extended thinking block (optional)

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
