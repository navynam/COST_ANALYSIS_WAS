package com.costanalysis.domain.verification.repository;

import com.costanalysis.domain.verification.entity.VerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationRepository extends JpaRepository<VerificationResult, Long> {
    Optional<VerificationResult> findTopByQuotation_IdOrderByCreatedAtDesc(Long quotationId);
}
