package com.costanalysis.domain.analysis.controller;

import com.costanalysis.domain.analysis.dto.AnalysisResult;
import com.costanalysis.domain.analysis.service.AnalysisService;
import com.costanalysis.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Analysis", description = "견적서 분석 API")
@RestController
@RequestMapping("/api/v1/quotations/{quotationId}/analysis")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "견적서 분석 실행")
    @GetMapping
    public ResponseEntity<ApiResponse<AnalysisResult>> analyze(@PathVariable Long quotationId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.analyze(quotationId)));
    }
}
