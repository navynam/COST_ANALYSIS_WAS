package com.costanalysis.domain.model.repository;

import com.costanalysis.domain.model.entity.CostFormula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostFormulaRepository extends JpaRepository<CostFormula, Long> {
    List<CostFormula> findByActiveOrderByCategory(boolean active);
    List<CostFormula> findByCategoryAndActive(String category, boolean active);
}
