package com.costanalysis.domain.quotation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "parsed_items")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ParsedItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id")
    private Quotation quotation;

    private Integer rowIndex;

    @Column(length = 200)
    private String itemCode;

    @Column(length = 500)
    private String itemName;

    @Column(length = 100)
    private String specification;

    @Column(length = 20)
    private String unit;

    private Integer quantity;

    @Column(precision = 18, scale = 4)
    private BigDecimal unitPrice;

    @Column(precision = 18, scale = 4)
    private BigDecimal totalPrice;

    @Column(length = 100)
    private String category;

    @Column(length = 500)
    private String notes;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidenceScore;  // AI 파싱 신뢰도 0.00~1.00

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;
}
