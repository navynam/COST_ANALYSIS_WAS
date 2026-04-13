package com.costanalysis.domain.analysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

/**
 * 피드백 Excel 생성 서비스
 *
 * <p>Apache POI를 사용하여 원본 Excel 파일의 양식/스타일/수식/도형/외부링크를
 * 100% 보존하면서 이상치 셀에 노트(Comment)와 하이라이트만 추가합니다.</p>
 *
 * <h3>왜 Apache POI만 사용하는가?</h3>
 * <ul>
 *   <li>openpyxl (Python): 외부 링크(externalLink)와 도형(drawing)을 손상시킴</li>
 *   <li>SheetJS (JavaScript): 셀 스타일/수식을 재생성하여 원본과 달라짐</li>
 *   <li>ExcelJS (JavaScript): 파일 구조를 재생성하여 "읽을 수 없는 내용" 오류 발생</li>
 *   <li>Apache POI: 원본 ZIP 구조를 그대로 유지하며 수정/추가만 처리</li>
 * </ul>
 *
 * <h3>처리 흐름</h3>
 * <pre>
 * 1. NAS에서 원본 Excel 파일 로드 (XSSFWorkbook)
 * 2. 분석 결과에서 이상치 목록 조회
 * 3. 각 이상치 셀에:
 *    - Excel 노트(Comment) 추가: 항목명, 카테고리, 신뢰도, 사유
 *    - 셀 하이라이트: 노란 배경 (기존 테두리/수식 보존)
 * 4. 바이트 배열로 출력 → 프론트엔드에서 다운로드
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackExcelService {

    /**
     * 이상치 피드백 정보
     *
     * @param cellRef    셀 주소 (예: "Z15", "I17")
     * @param itemName   항목명 (예: "SKIN (표피재)")
     * @param category   카테고리 경로 (예: "재료비 > 원자재")
     * @param confidence 신뢰도 (0~100%)
     * @param reason     이상치 사유 (AI 판단 근거)
     */
    public record AnomalyFeedback(
            String cellRef,
            String itemName,
            String category,
            int confidence,
            String reason
    ) {}

    /**
     * 원본 Excel에 피드백(노트 + 하이라이트)을 추가하여 바이트 배열로 반환
     *
     * @param originalFilePath 원본 Excel 파일 경로 (NAS 절대 경로)
     * @param anomalies        이상치 목록
     * @return 피드백이 추가된 Excel (.xlsx) 바이트 배열
     * @throws IOException 파일 읽기/쓰기 실패 시
     */
    public byte[] generateFeedbackExcel(String originalFilePath, List<AnomalyFeedback> anomalies)
            throws IOException {

        log.info("피드백 Excel 생성 시작: 원본={}, 이상치={}건", originalFilePath, anomalies.size());

        try (FileInputStream fis = new FileInputStream(originalFilePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            XSSFSheet sheet = workbook.getSheetAt(0);
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            CreationHelper factory = workbook.getCreationHelper();

            // ── 하이라이트용 스타일 생성 ──
            // 노란 배경만 적용하고, 볼드는 기존 폰트에 병합
            XSSFCellStyle highlightStyle = createHighlightStyle(workbook);

            // ── 각 이상치에 노트 + 하이라이트 적용 ──
            int noteCount = 0;
            for (AnomalyFeedback anomaly : anomalies) {
                boolean added = addFeedbackToCell(sheet, drawing, factory, highlightStyle, anomaly);
                if (added) noteCount++;
            }

            log.info("피드백 Excel 생성 완료: 노트 {}건 추가", noteCount);

            // ── 바이트 배열 출력 ──
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * InputStream에서 원본 로드 (MinIO/S3 등 스토리지 연동 시 사용)
     */
    public byte[] generateFeedbackExcel(InputStream originalStream, List<AnomalyFeedback> anomalies)
            throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook(originalStream)) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            CreationHelper factory = workbook.getCreationHelper();
            XSSFCellStyle highlightStyle = createHighlightStyle(workbook);

            for (AnomalyFeedback anomaly : anomalies) {
                addFeedbackToCell(sheet, drawing, factory, highlightStyle, anomaly);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    // ═══════════════════════════════════════════════════════
    // 내부 메서드
    // ═══════════════════════════════════════════════════════

    /**
     * 하이라이트 스타일 생성
     * - 노란 배경 (#FFFF00)
     * - 볼드 폰트
     * - 기본 테두리는 보존하지 않음 (셀별로 cloneStyleFrom 사용 권장)
     */
    private XSSFCellStyle createHighlightStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        // 기본 스타일 복사 (테두리 등)
        style.cloneStyleFrom(workbook.getCellStyleAt(0));

        // 노란 배경
        style.setFillForegroundColor(new XSSFColor(
                new byte[]{(byte) 255, (byte) 255, (byte) 0}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 볼드 폰트
        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 11);
        boldFont.setFontName("맑은 고딕");
        style.setFont(boldFont);

        return style;
    }

    /**
     * 특정 셀에 노트(Comment) + 하이라이트 추가
     *
     * @return 성공 여부
     */
    private boolean addFeedbackToCell(XSSFSheet sheet, XSSFDrawing drawing,
                                       CreationHelper factory, XSSFCellStyle highlightStyle,
                                       AnomalyFeedback anomaly) {
        try {
            CellReference ref = new CellReference(anomaly.cellRef());
            int rowIdx = ref.getRow();
            int colIdx = ref.getCol();

            // 행/셀 가져오기 (없으면 생성)
            Row row = sheet.getRow(rowIdx);
            if (row == null) row = sheet.createRow(rowIdx);
            Cell cell = row.getCell(colIdx);
            if (cell == null) cell = row.createCell(colIdx);

            // ── 1. 노트(Comment) 추가 ──
            // 기존 코멘트가 있으면 제거 후 재생성
            if (cell.getCellComment() != null) {
                cell.removeCellComment();
            }

            ClientAnchor anchor = factory.createClientAnchor();
            anchor.setCol1(colIdx + 1);
            anchor.setCol2(colIdx + 5);   // 노트 너비: 4열
            anchor.setRow1(rowIdx);
            anchor.setRow2(rowIdx + 8);   // 노트 높이: 8행

            Comment comment = drawing.createCellComment(anchor);

            // 리치 텍스트 (제목: 빨간 볼드, 본문: 기본)
            XSSFRichTextString richText = (XSSFRichTextString) factory.createRichTextString("");
            XSSFFont titleFont = sheet.getWorkbook().createFont();
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.RED.getIndex());
            titleFont.setFontHeightInPoints((short) 10);

            XSSFFont bodyFont = sheet.getWorkbook().createFont();
            bodyFont.setFontHeightInPoints((short) 9);
            bodyFont.setFontName("맑은 고딕");

            String title = "[이상치 감지]\n\n";
            String body = String.format(
                    "항목: %s\n카테고리: %s\n신뢰도: %d%%\n\n사유:\n%s\n\n— 원가분석시스템",
                    anomaly.itemName(), anomaly.category(),
                    anomaly.confidence(), anomaly.reason()
            );

            richText.append(title, titleFont);
            richText.append(body, bodyFont);

            comment.setString(richText);
            comment.setAuthor("원가분석시스템");
            cell.setCellComment(comment);

            // ── 2. 하이라이트 (기존 스타일에 배경색만 추가) ──
            // 기존 셀에 스타일이 있으면 복제 후 배경만 추가 (테두리/수식 보존)
            XSSFCellStyle existingStyle = (XSSFCellStyle) cell.getCellStyle();
            if (existingStyle != null && existingStyle.getIndex() != 0) {
                XSSFCellStyle mergedStyle = sheet.getWorkbook().createCellStyle();
                mergedStyle.cloneStyleFrom(existingStyle); // 기존 스타일 복사
                mergedStyle.setFillForegroundColor(new XSSFColor(
                        new byte[]{(byte) 255, (byte) 255, (byte) 0}, null));
                mergedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                cell.setCellStyle(mergedStyle);
            } else {
                cell.setCellStyle(highlightStyle);
            }

            log.debug("피드백 추가: {} - {} (신뢰도: {}%)", anomaly.cellRef(), anomaly.itemName(), anomaly.confidence());
            return true;

        } catch (Exception e) {
            log.error("피드백 추가 실패: {} - {}", anomaly.cellRef(), e.getMessage());
            return false;
        }
    }
}
