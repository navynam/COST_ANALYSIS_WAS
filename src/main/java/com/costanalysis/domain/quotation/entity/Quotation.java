package com.costanalysis.domain.quotation.entity;

import com.costanalysis.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Quotation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(nullable = false, length = 200)
    private String originalFilename;

    @Column(nullable = false)
    private String storagePath;

    @Column(nullable = false, length = 10)
    private String fileType;   // PDF | XLSX | XLS

    private Long fileSize;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "UPLOADED";   // UPLOADED | PARSING | PARSED | FAILED

    @Column(columnDefinition = "TEXT")
    private String parseErrorMessage;

    private Integer totalItems;

    @Column(length = 100)
    private String vendor;

    @Column(length = 200)
    private String title;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParsedItem> parsedItems = new ArrayList<>();
}
