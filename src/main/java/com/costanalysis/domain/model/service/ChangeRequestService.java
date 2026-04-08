package com.costanalysis.domain.model.service;

import com.costanalysis.domain.model.dto.ChangeRequestCreateDto;
import com.costanalysis.domain.model.dto.ChangeRequestResponse;
import com.costanalysis.domain.model.dto.ChangeRequestReviewDto;
import com.costanalysis.domain.model.entity.ChangeRequest;
import com.costanalysis.domain.model.repository.ChangeRequestRepository;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.domain.user.repository.UserRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@org.springframework.context.annotation.Profile("!mock")
public class ChangeRequestService {

    private final ChangeRequestRepository changeRequestRepository;
    private final UserRepository userRepository;

    /** 전체 변경 요청 목록 */
    public List<ChangeRequestResponse> listAll() {
        return changeRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(ChangeRequestResponse::from).toList();
    }

    /** 특정 수식의 변경 요청 목록 */
    public List<ChangeRequestResponse> listByFormula(Long formulaId) {
        return changeRequestRepository.findByFormulaIdOrderByCreatedAtDesc(formulaId)
                .stream().map(ChangeRequestResponse::from).toList();
    }

    /** 변경 요청 생성 */
    @Transactional
    public ChangeRequestResponse create(Long userId, ChangeRequestCreateDto dto) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ChangeRequest cr = ChangeRequest.builder()
                .formulaId(dto.getFormulaId())
                .requesterId(userId)
                .requesterName(requester.getName())
                .department(requester.getDepartment())
                .taskName(dto.getTaskName())
                .originalFormula(dto.getOriginalFormula())
                .modifiedFields(dto.getModifiedFields())
                .status("PENDING")
                .reason(dto.getReason())
                .build();

        return ChangeRequestResponse.from(changeRequestRepository.save(cr));
    }

    /** 승인 (관리자) */
    @Transactional
    public ChangeRequestResponse approve(Long id, ChangeRequestReviewDto dto) {
        ChangeRequest cr = findById(id);
        validatePending(cr);

        cr.setStatus("APPROVED");
        cr.setReviewedAt(OffsetDateTime.now());
        cr.setReviewerComment(dto.getComment());
        cr.setApprovedDepartments(dto.getApprovedDepartments());

        return ChangeRequestResponse.from(changeRequestRepository.save(cr));
    }

    /** 반려 (관리자) */
    @Transactional
    public ChangeRequestResponse reject(Long id, ChangeRequestReviewDto dto) {
        ChangeRequest cr = findById(id);
        validatePending(cr);

        cr.setStatus("REJECTED");
        cr.setReviewedAt(OffsetDateTime.now());
        cr.setReviewerComment(dto.getComment());

        return ChangeRequestResponse.from(changeRequestRepository.save(cr));
    }

    /** 삭제 (본인 대기중 요청만) */
    @Transactional
    public void delete(Long id, Long userId) {
        ChangeRequest cr = findById(id);
        if (!cr.getRequesterId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!"PENDING".equals(cr.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "대기 중인 요청만 삭제할 수 있습니다.");
        }
        changeRequestRepository.delete(cr);
    }

    // ── helpers ──

    private ChangeRequest findById(Long id) {
        return changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHANGE_REQUEST_NOT_FOUND));
    }

    private void validatePending(ChangeRequest cr) {
        if (!"PENDING".equals(cr.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "이미 처리된 요청입니다.");
        }
    }
}
