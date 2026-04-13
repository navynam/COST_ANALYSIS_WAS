package com.costanalysis.domain.quotation.controller;

import com.costanalysis.domain.quotation.service.ExcelStyleService;
import com.costanalysis.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Excel 스타일 JSON API
 *
 * 프론트엔드(Handsontable)에서 Excel 원본을 렌더링하기 위한
 * 셀 값 + 스타일 + 병합 + 열 너비/행 높이 + 수식 데이터를 반환합니다.
 */
@Tag(name = "Excel", description = "Excel 스타일 파싱 API")
@RestController
@RequestMapping("/api/v1/excel")
@RequiredArgsConstructor
public class ExcelStyleController {

    private final ExcelStyleService excelStyleService;

    @Operation(summary = "Excel 스타일 JSON 조회",
            description = "견적서 원본 Excel 파일의 셀 값 + 스타일(배경색, 폰트, 테두리, 정렬) + " +
                    "병합 + 열 너비/행 높이 + 수식을 JSON으로 반환합니다.")
    @GetMapping("/styles")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExcelStyles(
            @RequestParam(required = false) Long quotationId,
            @RequestParam(required = false) Integer sheetIndex
    ) {
        try {
            // TODO: 실제 운영 시 quotationId로 NAS 파일 경로 조회
            // String filePath = quotationService.getFilePath(quotationId);
            String filePath = "sample_data.xlsx"; // 데모용

            Map<String, Object> result = excelStyleService.parseExcelStyles(filePath, sheetIndex);
            return ResponseEntity.ok(ApiResponse.ok(result));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Excel 파싱 실패: " + e.getMessage()));
        }
    }
}
