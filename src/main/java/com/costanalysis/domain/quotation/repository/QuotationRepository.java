package com.costanalysis.domain.quotation.repository;

import com.costanalysis.domain.quotation.entity.Quotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Page<Quotation> findByUploadedBy_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Quotation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT q FROM Quotation q LEFT JOIN FETCH q.parsedItems WHERE q.id = :id")
    Optional<Quotation> findByIdWithItems(@Param("id") Long id);
}
