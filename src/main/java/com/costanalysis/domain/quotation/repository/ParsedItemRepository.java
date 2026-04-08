package com.costanalysis.domain.quotation.repository;

import com.costanalysis.domain.quotation.entity.ParsedItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParsedItemRepository extends JpaRepository<ParsedItem, Long> {
    List<ParsedItem> findByQuotation_IdOrderByRowIndex(Long quotationId);
    void deleteByQuotation_Id(Long quotationId);
}
