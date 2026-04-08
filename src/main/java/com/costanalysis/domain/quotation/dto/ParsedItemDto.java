package com.costanalysis.domain.quotation.dto;

import com.costanalysis.domain.quotation.entity.ParsedItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @Builder
public class ParsedItemDto {
    private Long       id;
    private Integer    rowIndex;
    private String     itemCode;
    private String     itemName;
    private String     specification;
    private String     unit;
    private Integer    quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String     category;
    private String     notes;
    private BigDecimal confidenceScore;
    private boolean    verified;

    public static ParsedItemDto from(ParsedItem item) {
        return ParsedItemDto.builder()
                .id(item.getId())
                .rowIndex(item.getRowIndex())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .specification(item.getSpecification())
                .unit(item.getUnit())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .category(item.getCategory())
                .notes(item.getNotes())
                .confidenceScore(item.getConfidenceScore())
                .verified(item.isVerified())
                .build();
    }
}
