package com.costanalysis.domain.model.controller;

import com.costanalysis.domain.model.dto.CostFormulaRequest;
import com.costanalysis.domain.model.dto.CostFormulaResponse;
import com.costanalysis.domain.model.service.CostModelService;
import com.costanalysis.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "CostModel", description = "원가 수식 관리 API")
@RestController
@RequestMapping("/api/v1/formulas")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class CostModelController {

    private final CostModelService costModelService;

    @Operation(summary = "수식 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CostFormulaResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(costModelService.listAll()));
    }

    @Operation(summary = "수식 상세")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CostFormulaResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(costModelService.getById(id)));
    }

    @Operation(summary = "수식 생성 (ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CostFormulaResponse>> create(@Valid @RequestBody CostFormulaRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(costModelService.create(req)));
    }

    @Operation(summary = "수식 수정 (ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CostFormulaResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CostFormulaRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(costModelService.update(id, req)));
    }

    @Operation(summary = "수식 삭제 (ADMIN, 시스템 수식 불가)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        costModelService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("수식이 삭제되었습니다."));
    }
}
