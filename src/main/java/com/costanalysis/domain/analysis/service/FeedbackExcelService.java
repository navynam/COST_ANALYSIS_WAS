package com.costanalysis.domain.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

/**
 * 피드백 Excel 생성 서비스
 *
 * Apache POI를 사용하여 원본 Excel 파일의 양식/스타일/수식/도형/외부링크를
 * 100% 보존하면서 이상치 셀에 노트(Comment)와 하이라이트만 추가합니다.
 *
 * ※ openpyxl, SheetJS 등 다른 라이브러리는 원본 구조를 손상시키므로
 *    Apache POI XSSFWorkbook만 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackExcelService {

    /**
     * 이상치 정보
     */
    public record AnomalyFeedback(
            String cellRef,     // 셀 주소 (예: "Z15")
            String itemName,    // 항목명
            String category,    // 카테고리
            int confidence,     // 신뢰도 (%)
            String reason       // 이상치 사유
    ) {}

    /**
     * 원본 Excel에 피드백(노트 + 하이라이트) 추가
     *
     * @param originalFile 원본 Excel 파일 경로 (NAS)
     * @param anomalies    이상치 목록
     * @return 피드백이 추가된 Excel 바이트 배열
     */
    public byte[] generateFeedbackExcel(String originalFile, List<AnomalyFeedback> anomalies) throws IOException {
        try (FileInputStream fis = new FileInputStream(originalFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);

            // 코멘트용 Drawing 생성
            XSSFDrawing drawing = sheet.createDrawingPatriarch();

            // 하이라이트 스타일 생성 (노란 배경 + 볼드)
            XSSFCellStyle highlightStyle = workbook.createCellStyle();
            highlightStyle.cloneStyleFrom(workbook.getCellStyleAt(0)); // 기본 스타일 복사
            highlightStyle.setFillForegroundColor(new XSSFColor(
                    new byte[]{(byte) 255, (byte) 255, (byte) 0}, null)); // 노란색
            highlightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 11);
            boldFont.setFontName("맑은 고딕");
            highlightStyle.setFont(boldFont);

            // 각 이상치에 노트 + 하이라이트 적용
            for (AnomalyFeedback anomaly : anomalies) {
                addFeedbackToCell(sheet, drawing, highlightStyle, anomaly);
            }

            // 바이트 배열로 출력
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * 특정 셀에 노트 + 하이라이트 추가
     */
    private void addFeedbackToCell(XSSFSheet sheet, XSSFDrawing drawing,
                                    XSSFCellStyle highlightStyle, AnomalyFeedback anomaly) {
        // 셀 주소 파싱
        org.apache.poi.ss.util.CellReference ref =
                new org.apache.poi.ss.util.CellReference(anomaly.cellRef());
        int rowIdx = ref.getRow();
        int colIdx = ref.getCol();

        // 행/셀 가져오기 (없으면 생성)
        Row row = sheet.getRow(rowIdx);
        if (row == null) row = sheet.createRow(rowIdx);
        Cell cell = row.getCell(colIdx);
        if (cell == null) cell = row.createCell(colIdx);

        // 1. 노트(Comment) 추가
        CreationHelper factory = sheet.getWorkbook().getCreationHelper();
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(colIdx + 1);
        anchor.setCol2(colIdx + 5);
        anchor.setRow1(rowIdx);
        anchor.setRow2(rowIdx + 8);

        Comment comment = drawing.createCellComment(anchor);
        String noteText = String.format(
                "[이상치 감지]\n\n" +
                        "항목: %s\n" +
                        "카테고리: %s\n" +
                        "신뢰도: %d%%\n\n" +
                        "사유:\n%s\n\n" +
                        "— 원가분석시스템 자동 생성",
                anomaly.itemName(), anomaly.category(),
                anomaly.confidence(), anomaly.reason()
        );
        comment.setString(factory.createRichTextString(noteText));
        comment.setAuthor("원가분석시스템");
        cell.setCellComment(comment);

        // 2. 하이라이트 (배경색 + 볼드)
        cell.setCellStyle(highlightStyle);

        log.info("피드백 추가: {} - {}", anomaly.cellRef(), anomaly.itemName());
    }
}
