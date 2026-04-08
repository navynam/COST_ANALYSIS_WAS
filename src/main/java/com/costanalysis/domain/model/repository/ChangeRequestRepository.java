package com.costanalysis.domain.model.repository;

import com.costanalysis.domain.model.entity.ChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    List<ChangeRequest> findByFormulaIdOrderByCreatedAtDesc(Long formulaId);

    List<ChangeRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<ChangeRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<ChangeRequest> findAllByOrderByCreatedAtDesc();

    long countByFormulaIdAndStatus(Long formulaId, String status);
}
