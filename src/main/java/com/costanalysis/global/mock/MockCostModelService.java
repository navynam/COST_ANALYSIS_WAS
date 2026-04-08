package com.costanalysis.global.mock;

import com.costanalysis.domain.model.dto.CostFormulaRequest;
import com.costanalysis.domain.model.dto.CostFormulaResponse;
import com.costanalysis.domain.model.service.CostModelService;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock 프로파일 전용 원가 수식 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockCostModelService extends CostModelService {

    private final MockDataStore dataStore;

    public MockCostModelService(MockDataStore dataStore) {
        super(null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public List<CostFormulaResponse> listAll() {
        return dataStore.getFormulas().values().stream()
                .filter(f -> (boolean) f.get("active"))
                .sorted(Comparator.comparing(f -> (String) f.get("category")))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CostFormulaResponse getById(Long id) {
        Map<String, Object> f = dataStore.getFormulas().get(id);
        if (f == null) throw new BusinessException(ErrorCode.FORMULA_NOT_FOUND);
        return toResponse(f);
    }

    @Override
    public CostFormulaResponse create(CostFormulaRequest req) {
        Long id = dataStore.nextFormulaId();
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> f = new java.util.concurrent.ConcurrentHashMap<>();
        f.put("id", id);
        f.put("name", req.getName());
        f.put("category", req.getCategory());
        f.put("formula", req.getFormula());
        f.put("description", req.getDescription());
        f.put("systemFormula", false);
        f.put("active", req.getActive() != null ? req.getActive() : true);
        f.put("departments", req.getDepartments() != null ? new ArrayList<>(req.getDepartments()) : new ArrayList<>(List.of("전체")));
        f.put("createdAt", now);
        f.put("updatedAt", now);
        dataStore.getFormulas().put(id, f);
        log.debug("[MOCK] 수식 생성: {} (id={})", req.getName(), id);
        return toResponse(f);
    }

    @Override
    public CostFormulaResponse update(Long id, CostFormulaRequest req) {
        Map<String, Object> f = dataStore.getFormulas().get(id);
        if (f == null) throw new BusinessException(ErrorCode.FORMULA_NOT_FOUND);

        f.put("name", req.getName());
        f.put("category", req.getCategory());
        f.put("formula", req.getFormula());
        f.put("description", req.getDescription());
        if (req.getActive() != null) f.put("active", req.getActive());
        if (req.getDepartments() != null) f.put("departments", new ArrayList<>(req.getDepartments()));
        f.put("updatedAt", OffsetDateTime.now());
        log.debug("[MOCK] 수식 수정: {} (id={})", req.getName(), id);
        return toResponse(f);
    }

    @Override
    public void delete(Long id) {
        Map<String, Object> f = dataStore.getFormulas().get(id);
        if (f == null) throw new BusinessException(ErrorCode.FORMULA_NOT_FOUND);
        if ((boolean) f.get("systemFormula")) {
            throw new BusinessException(ErrorCode.SYSTEM_FORMULA_PROTECTED);
        }
        dataStore.getFormulas().remove(id);
        log.debug("[MOCK] 수식 삭제: id={}", id);
    }

    @SuppressWarnings("unchecked")
    private CostFormulaResponse toResponse(Map<String, Object> f) {
        return CostFormulaResponse.builder()
                .id((Long) f.get("id"))
                .name((String) f.get("name"))
                .category((String) f.get("category"))
                .formula((String) f.get("formula"))
                .description((String) f.get("description"))
                .systemFormula((boolean) f.get("systemFormula"))
                .active((boolean) f.get("active"))
                .departments((List<String>) f.get("departments"))
                .createdAt((OffsetDateTime) f.get("createdAt"))
                .updatedAt((OffsetDateTime) f.get("updatedAt"))
                .build();
    }
}
