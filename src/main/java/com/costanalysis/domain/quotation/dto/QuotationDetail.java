package com.costanalysis.domain.quotation.dto;

import com.costanalysis.domain.quotation.entity.Quotation;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter @Builder
public class QuotationDetail {
    private Long   id;
    private String originalFilename;
    private String fileType;
    private Long   fileSize;
    private String status;
    private String vendor;
    private String title;
    private Integer totalItems;
    private String parseErrorMessage;
    private String uploaderName;
    private String downloadUrl;
    private OffsetDateTime createdAt;
    private List<ParsedItemDto> items;

    public static QuotationDetail from(Quotation q, String downloadUrl) {
        return QuotationDetail.builder()
                .id(q.getId())
                .originalFilename(q.getOriginalFilename())
                .fileType(q.getFileType())
                .fileSize(q.getFileSize())
                .status(q.getStatus())
                .vendor(q.getVendor())
                .title(q.getTitle())
                .totalItems(q.getTotalItems())
                .parseErrorMessage(q.getParseErrorMessage())
                .uploaderName(q.getUploadedBy().getName())
                .downloadUrl(downloadUrl)
                .createdAt(q.getCreatedAt())
                .items(q.getParsedItems().stream().map(ParsedItemDto::from).toList())
                .build();
    }
}
