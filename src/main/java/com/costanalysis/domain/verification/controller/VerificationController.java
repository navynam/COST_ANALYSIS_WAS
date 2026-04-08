package com.costanalysis.domain.verification.controller;

import com.costanalysis.domain.verification.dto.VerificationDecisionRequest;
import com.costanalysis.domain.verification.dto.VerificationResponse;
import com.costanalysis.domain.verification.service.VerificationService;
import com.costanalysis.global.response.ApiResponse;
import com.costanalysis.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Verification", description = "견적서 검증 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class VerificationController {

    private final VerificationService verificationService;
    private final JwtTokenProvider    jwtTokenProvider;

    @Operation(summary = "검증 결과 자동 생성")
    @PostMapping("/quotations/{quotationId}/verification")
    public ResponseEntity<ApiResponse<VerificationResponse>> create(@PathVariable Long quotationId) {
        return ResponseEntity.ok(ApiResponse.ok(verificationService.createVerification(quotationId)));
    }

    @Operation(summary = "최신 검증 결과 조회")
    @GetMapping("/quotations/{quotationId}/verification")
    public ResponseEntity<ApiResponse<VerificationResponse>> getLatest(@PathVariable Long quotationId) {
        return ResponseEntity.ok(ApiResponse.ok(verificationService.getLatest(quotationId)));
    }

    @Operation(summary = "검증 결정 (승인/반려)")
    @PutMapping("/verifications/{verificationId}/decision")
    public ResponseEntity<ApiResponse<VerificationResponse>> decide(
            @PathVariable Long verificationId,
            @Valid @RequestBody VerificationDecisionRequest req,
            HttpServletRequest httpReq) {
        Long userId = jwtTokenProvider.getUserId(resolveToken(httpReq));
        return ResponseEntity.ok(ApiResponse.ok(verificationService.decide(verificationId, userId, req)));
    }

    private String resolveToken(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : "";
    }
}
