package com.costanalysis.domain.quotation.controller;

import com.costanalysis.domain.quotation.entity.Quotation;
import com.costanalysis.domain.quotation.service.QuotationParsingService;
import com.costanalysis.domain.quotation.service.QuotationService;
import com.costanalysis.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Quotation Parsing", description = "견적서 업로드 및 파싱 SSE")
@RestController
@RequestMapping("/api/v1/quotations")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class QuotationParsingController {

    private final QuotationService        quotationService;
    private final QuotationParsingService parsingService;
    private final JwtTokenProvider        jwtTokenProvider;

    /**
     * 1단계: 파일 업로드 → quotationId 반환
     */
    @Operation(summary = "견적서 파일 업로드")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long upload(@RequestParam("file") MultipartFile file, HttpServletRequest req) {
        Long userId = jwtTokenProvider.getUserId(resolveToken(req));
        Quotation q = quotationService.upload(userId, file);
        return q.getId();
    }

    /**
     * 2단계: SSE 스트림으로 파싱 진행률 수신
     */
    @Operation(summary = "견적서 파싱 진행 SSE")
    @GetMapping(value = "/{quotationId}/parse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter parse(@PathVariable Long quotationId) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5분 타임아웃
        parsingService.parseAsync(quotationId, emitter);
        return emitter;
    }

    private String resolveToken(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : "";
    }
}
