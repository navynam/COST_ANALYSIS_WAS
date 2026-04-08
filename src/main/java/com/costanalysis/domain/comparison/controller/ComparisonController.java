package com.costanalysis.domain.comparison.controller;

import com.costanalysis.domain.comparison.dto.ComparisonCreateRequest;
import com.costanalysis.domain.comparison.dto.ComparisonResponse;
import com.costanalysis.domain.comparison.service.ComparisonService;
import com.costanalysis.global.response.ApiResponse;
import com.costanalysis.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Comparison", description = "견적서 비교 API")
@RestController
@RequestMapping("/api/v1/comparisons")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ComparisonController {

    private final ComparisonService comparisonService;
    private final JwtTokenProvider  jwtTokenProvider;

    @Operation(summary = "비교 세션 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<ComparisonResponse>> create(
            @Valid @RequestBody ComparisonCreateRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(ApiResponse.ok(comparisonService.create(userId(httpReq), req)));
    }

    @Operation(summary = "내 비교 세션 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ComparisonResponse>>> listMine(
            HttpServletRequest httpReq,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(comparisonService.listMine(userId(httpReq), pageable)));
    }

    @Operation(summary = "비교 세션 상세")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ComparisonResponse>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(comparisonService.getDetail(id)));
    }

    @Operation(summary = "비교 세션 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        comparisonService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("비교 세션이 삭제되었습니다."));
    }

    private Long userId(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        String token  = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : "";
        return jwtTokenProvider.getUserId(token);
    }
}
