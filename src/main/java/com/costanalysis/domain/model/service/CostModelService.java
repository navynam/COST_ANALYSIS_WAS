package com.costanalysis.domain.model.service;

import com.costanalysis.domain.model.dto.CostFormulaRequest;
import com.costanalysis.domain.model.dto.CostFormulaResponse;
import com.costanalysis.domain.model.entity.CostFormula;
import com.costanalysis.domain.model.repository.CostFormulaRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@org.springframework.context.annotation.Profile("!mock")
public class CostModelService {

    private final CostFormulaRepository formulaRepository;

    public List<CostFormulaResponse> listAll() {
        return formulaRepository.findByActiveOrderByCategory(true)
                .stream().map(CostFormulaResponse::from).toList();
    }

    public CostFormulaResponse getById(Long id) {
        return CostFormulaResponse.from(find(id));
    }

    @Transactional
    public CostFormulaResponse create(CostFormulaRequest req) {
        CostFormula formula = CostFormula.builder()
                .name(req.getName())
                .category(req.getCategory())
                .formula(req.getFormula())
                .description(req.getDescription())
                .systemFormula(false)
                .active(req.getActive() != null ? req.getActive() : true)
                .departments(req.getDepartments() != null ? req.getDepartments() : new java.util.ArrayList<>())
                .build();
        return CostFormulaResponse.from(formulaRepository.save(formula));
    }

    @Transactional
    public CostFormulaResponse update(Long id, CostFormulaRequest req) {
        CostFormula formula = find(id);
        formula.setName(req.getName());
        formula.setCategory(req.getCategory());
        formula.setFormula(req.getFormula());
        formula.setDescription(req.getDescription());
        if (req.getActive() != null) formula.setActive(req.getActive());
        if (req.getDepartments() != null) formula.setDepartments(req.getDepartments());
        return CostFormulaResponse.from(formulaRepository.save(formula));
    }

    @Transactional
    public void delete(Long id) {
        CostFormula formula = find(id);
        if (formula.isSystemFormula()) {
            throw new BusinessException(ErrorCode.SYSTEM_FORMULA_PROTECTED);
        }
        formulaRepository.delete(formula);
    }

    private CostFormula find(Long id) {
        return formulaRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORMULA_NOT_FOUND));
    }
}
