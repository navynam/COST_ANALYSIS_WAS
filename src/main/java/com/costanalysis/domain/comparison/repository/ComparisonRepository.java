package com.costanalysis.domain.comparison.repository;

import com.costanalysis.domain.comparison.entity.ComparisonSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComparisonRepository extends JpaRepository<ComparisonSession, Long> {
    Page<ComparisonSession> findByCreatedBy_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
