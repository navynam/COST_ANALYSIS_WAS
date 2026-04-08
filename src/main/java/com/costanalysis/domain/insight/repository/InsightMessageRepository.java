package com.costanalysis.domain.insight.repository;

import com.costanalysis.domain.insight.entity.InsightMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsightMessageRepository extends JpaRepository<InsightMessage, Long> {
    List<InsightMessage> findBySession_IdOrderByCreatedAt(Long sessionId);
}
