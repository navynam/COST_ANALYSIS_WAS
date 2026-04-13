package com.costanalysis.domain.analysis.controller;

import com.costanalysis.domain.analysis.dto.AnalysisResult;
import com.costanalysis.domain.analysis.service.AnalysisService;
import com.costanalysis.domain.analysis.service.FeedbackExcelService;
import com.costanalysis.domain.analysis.service.FeedbackExcelService.AnomalyFeedback;
import com.costanalysis.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Analysis", description = "견적서 분석 API")
@RestController
@RequestMapping("/api/v1/quotations/{quotationId}/analysis")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final FeedbackExcelService feedbackExcelService;

    @Operation(summary = "견적서 분석 실행")
    @GetMapping
    public ResponseEntity<ApiResponse<AnalysisResult>> analyze(@PathVariable Long quotationId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.analyze(quotationId)));
    }

    @Operation(summary = "피드백 Excel 다운로드",
            description = "원본 Excel에 이상치 노트(코멘트) + 셀 하이라이트를 추가하여 다운로드합니다. " +
                    "원본 양식/수식/도형은 100% 보존됩니다.")
    @GetMapping("/feedback-excel")
    public ResponseEntity<byte[]> downloadFeedbackExcel(@PathVariable Long quotationId) {
        try {
            // TODO: 실제 운영 시 quotationId로 NAS에서 원본 파일 경로 조회
            // String originalFilePath = quotationService.getFilePath(quotationId);
            // List<AnomalyFeedback> anomalies = analysisService.getAnomalies(quotationId);

            // 데모: 샘플 파일 + 샘플 이상치
            String originalFilePath = "sample_data.xlsx"; // NAS 경로
            List<AnomalyFeedback> anomalies = List.of(
                    new AnomalyFeedback("Z15", "SKIN (표피재)", "재료비 > 원자재", 75,
                            "과거 3개 분기 평균 단가 ₩14,200 대비 +33.2% 높음. " +
                                    "동일 규격(PVC 0.8mm) 시장가 범위: ₩13,500~₩16,800."),
                    new AnomalyFeedback("Z37", "기타 경비", "제경비 > 기타", 72,
                            "기타 경비가 전체 제경비의 29.4%를 차지하여 " +
                                    "일반적 범위(10~20%)를 초과합니다.")
            );

            byte[] excelBytes = feedbackExcelService.generateFeedbackExcel(originalFilePath, anomalies);

            String fileName = URLEncoder.encode(
                    "피드백_" + LocalDate.now() + "_견적서.xlsx",
                    StandardCharsets.UTF_8
            ).replace("+", "%20");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
