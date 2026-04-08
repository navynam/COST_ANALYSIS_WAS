package com.costanalysis.domain.quotation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "parsing_notes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ParsingNote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** parsing | upload | format | improvement */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String type = "parsing";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
