package com.costanalysis.global.mock;

import com.costanalysis.domain.model.dto.ChangeRequestCreateDto;
import com.costanalysis.domain.model.dto.ChangeRequestResponse;
import com.costanalysis.domain.model.dto.ChangeRequestReviewDto;
import com.costanalysis.domain.model.service.ChangeRequestService;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock 프로파일 전용 변경 요청 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockChangeRequestService extends ChangeRequestService {

    private final MockDataStore dataStore;

    public MockChangeRequestService(MockDataStore dataStore) {
        super(null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public List<ChangeRequestResponse> listAll() {
        return dataStore.getChangeRequests().values().stream()
                .sorted(Comparator.comparing((Map<String, Object> cr) -> (OffsetDateTime) cr.get("createdAt")).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChangeRequestResponse> listByFormula(Long formulaId) {
        return dataStore.getChangeRequests().values().stream()
                .filter(cr -> formulaId.equals(cr.get("formulaId")))
                .sorted(Comparator.comparing((Map<String, Object> cr) -> (OffsetDateTime) cr.get("createdAt")).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ChangeRequestResponse create(Long userId, ChangeRequestCreateDto dto) {
        User user = dataStore.getUsers().get(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        Long id = dataStore.nextChangeReqId();
        Map<String, Object> cr = new java.util.concurrent.ConcurrentHashMap<>();
        cr.put("id", id);
        cr.put("formulaId", dto.getFormulaId());
        cr.put("requesterId", userId);
        cr.put("requesterName", user.getName());
        cr.put("department", user.getDepartment());
        cr.put("taskName", dto.getTaskName());
        cr.put("originalFormula", dto.getOriginalFormula());
        cr.put("modifiedFields", dto.getModifiedFields());
        cr.put("status", "PENDING");
        cr.put("reason", dto.getReason());
        cr.put("createdAt", OffsetDateTime.now());
        // ConcurrentHashMap does not allow null values; omit null fields
        dataStore.getChangeRequests().put(id, cr);
        log.debug("[MOCK] 변경 요청 생성: id={}", id);
        return toResponse(cr);
    }

    @Override
    public ChangeRequestResponse approve(Long id, ChangeRequestReviewDto dto) {
        Map<String, Object> cr = findById(id);
        validatePending(cr);
        cr.put("status", "APPROVED");
        cr.put("reviewedAt", OffsetDateTime.now());
        cr.put("reviewerComment", dto.getComment());
        cr.put("approvedDepartments", dto.getApprovedDepartments());
        log.debug("[MOCK] 변경 요청 승인: id={}", id);
        return toResponse(cr);
    }

    @Override
    public ChangeRequestResponse reject(Long id, ChangeRequestReviewDto dto) {
        Map<String, Object> cr = findById(id);
        validatePending(cr);
        cr.put("status", "REJECTED");
        cr.put("reviewedAt", OffsetDateTime.now());
        cr.put("reviewerComment", dto.getComment());
        log.debug("[MOCK] 변경 요청 반려: id={}", id);
        return toResponse(cr);
    }

    @Override
    public void delete(Long id, Long userId) {
        Map<String, Object> cr = findById(id);
        if (!userId.equals(cr.get("requesterId"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!"PENDING".equals(cr.get("status"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "대기 중인 요청만 삭제할 수 있습니다.");
        }
        dataStore.getChangeRequests().remove(id);
        log.debug("[MOCK] 변경 요청 삭제: id={}", id);
    }

    // ── 헬퍼 ──

    private Map<String, Object> findById(Long id) {
        Map<String, Object> cr = dataStore.getChangeRequests().get(id);
        if (cr == null) throw new BusinessException(ErrorCode.CHANGE_REQUEST_NOT_FOUND);
        return cr;
    }

    private void validatePending(Map<String, Object> cr) {
        if (!"PENDING".equals(cr.get("status"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "이미 처리된 요청입니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private ChangeRequestResponse toResponse(Map<String, Object> cr) {
        return ChangeRequestResponse.builder()
                .id((Long) cr.get("id"))
                .formulaId((Long) cr.get("formulaId"))
                .requesterId((Long) cr.get("requesterId"))
                .requesterName((String) cr.get("requesterName"))
                .department((String) cr.get("department"))
                .taskName((String) cr.get("taskName"))
                .originalFormula((String) cr.get("originalFormula"))
                .modifiedFields((String) cr.get("modifiedFields"))
                .status((String) cr.get("status"))
                .reason((String) cr.get("reason"))
                .createdAt((OffsetDateTime) cr.get("createdAt"))
                .reviewedAt((OffsetDateTime) cr.get("reviewedAt"))
                .reviewerComment((String) cr.get("reviewerComment"))
                .approvedDepartments((List<String>) cr.get("approvedDepartments"))
                .build();
    }
}
