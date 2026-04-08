package com.costanalysis.domain.quotation.dto;

import com.costanalysis.domain.quotation.entity.Quotation;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter @Builder
public class QuotationSummary {
    private Long   id;
    private String originalFilename;
    private String fileType;
    private Long   fileSize;
    private String status;
    private String vendor;
    private String title;
    private Integer totalItems;
    private String uploaderName;
    private OffsetDateTime createdAt;

    public static QuotationSummary from(Quotation q) {
        return QuotationSummary.builder()
                .id(q.getId())
                .originalFilename(q.getOriginalFilename())
                .fileType(q.getFileType())
                .fileSize(q.getFileSize())
                .status(q.getStatus())
                .vendor(q.getVendor())
                .title(q.getTitle())
                .totalItems(q.getTotalItems())
                .uploaderName(q.getUploadedBy().getName())
                .createdAt(q.getCreatedAt())
                .build();
    }
}
