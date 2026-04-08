package com.costanalysis.domain.model.controller;

import com.costanalysis.domain.model.dto.ChangeRequestCreateDto;
import com.costanalysis.domain.model.dto.ChangeRequestResponse;
import com.costanalysis.domain.model.dto.ChangeRequestReviewDto;
import com.costanalysis.domain.model.service.ChangeRequestService;
import com.costanalysis.global.response.ApiResponse;
import com.costanalysis.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ChangeRequest", description = "모델 변경 요청 워크플로우 API")
@RestController
@RequestMapping("/api/v1/models/change-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "변경 요청 전체 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChangeRequestResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(changeRequestService.listAll()));
    }

    @Operation(summary = "특정 수식의 변경 요청 목록")
    @GetMapping("/formula/{formulaId}")
    public ResponseEntity<ApiResponse<List<ChangeRequestResponse>>> listByFormula(
            @PathVariable Long formulaId) {
        return ResponseEntity.ok(ApiResponse.ok(changeRequestService.listByFormula(formulaId)));
    }

    @Operation(summary = "변경 요청 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<ChangeRequestResponse>> create(
            HttpServletRequest req,
            @Valid @RequestBody ChangeRequestCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(changeRequestService.create(userId(req), dto)));
    }

    @Operation(summary = "변경 요청 승인 (ADMIN)")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChangeRequestResponse>> approve(
            @PathVariable Long id,
            @RequestBody ChangeRequestReviewDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(changeRequestService.approve(id, dto)));
    }

    @Operation(summary = "변경 요청 반려 (ADMIN)")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ChangeRequestResponse>> reject(
            @PathVariable Long id,
            @RequestBody ChangeRequestReviewDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(changeRequestService.reject(id, dto)));
    }

    @Operation(summary = "변경 요청 삭제 (본인 대기중 요청만)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            HttpServletRequest req) {
        changeRequestService.delete(id, userId(req));
        return ResponseEntity.ok(ApiResponse.ok("변경 요청이 삭제되었습니다."));
    }

    private Long userId(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        String token = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : "";
        return jwtTokenProvider.getUserId(token);
    }
}
