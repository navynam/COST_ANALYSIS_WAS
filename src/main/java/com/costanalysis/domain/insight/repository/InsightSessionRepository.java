package com.costanalysis.domain.insight.repository;

import com.costanalysis.domain.insight.entity.InsightSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightSessionRepository extends JpaRepository<InsightSession, Long> {
    Page<InsightSession> findByUser_IdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
}
