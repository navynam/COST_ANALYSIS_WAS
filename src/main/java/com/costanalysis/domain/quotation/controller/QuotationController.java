package com.costanalysis.domain.quotation.controller;

import com.costanalysis.domain.quotation.dto.ParsedItemDto;
import com.costanalysis.domain.quotation.dto.QuotationDetail;
import com.costanalysis.domain.quotation.dto.QuotationSummary;
import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.service.QuotationService;
import com.costanalysis.global.response.ApiResponse;
import com.costanalysis.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Quotation", description = "견적서 API")
@RestController
@RequestMapping("/api/v1/quotations")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class QuotationController {

    private final QuotationService quotationService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "내 견적서 목록")
    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<Page<QuotationSummary>>> listMine(
            HttpServletRequest req,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(quotationService.listMine(userId(req), pageable)));
    }

    @Operation(summary = "전체 견적서 목록 (ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<QuotationSummary>>> listAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(quotationService.listAll(pageable)));
    }

    @Operation(summary = "견적서 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuotationDetail>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(quotationService.getDetail(id)));
    }

    @Operation(summary = "견적서 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, HttpServletRequest req) {
        String token = resolveToken(req);
        boolean isAdmin = "ADMIN".equals(jwtTokenProvider.getRole(token));
        quotationService.delete(id, userId(req), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("견적서가 삭제되었습니다."));
    }

    @Operation(summary = "파싱 항목 수정")
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<ParsedItemDto>> updateItem(
            @PathVariable Long itemId,
            @RequestBody ParsedItem patch) {
        return ResponseEntity.ok(ApiResponse.ok(ParsedItemDto.from(quotationService.updateItem(itemId, patch))));
    }

    private Long userId(HttpServletRequest req) {
        return jwtTokenProvider.getUserId(resolveToken(req));
    }

    private String resolveToken(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : "";
    }
}
