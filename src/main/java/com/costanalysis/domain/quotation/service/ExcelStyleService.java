package com.costanalysis.domain.quotation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

/**
 * Excel 파일의 셀 스타일을 JSON으로 추출하는 서비스
 *
 * Apache POI로 원본 Excel을 파싱하여:
 * - 셀 값 (formatted)
 * - 셀 스타일 (배경색, 글자색, 볼드, 이탤릭, 정렬, 테두리)
 * - 병합 셀
 * - 열 너비 / 행 높이
 * - 수식
 * 을 JSON 형태로 반환합니다.
 *
 * 프론트엔드(Handsontable)에서 이 JSON을 받아 Excel과 동일하게 렌더링합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelStyleService {

    /**
     * Excel 파일을 파싱하여 시트별 스타일 정보를 반환
     *
     * @param filePath NAS 파일 경로
     * @param sheetIndex 시트 인덱스 (0-based, null이면 전체)
     * @return 시트별 스타일 데이터
     */
    public Map<String, Object> parseExcelStyles(String filePath, Integer sheetIndex) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            return parseWorkbook(workbook, sheetIndex);
        }
    }

    /**
     * InputStream에서 파싱 (MinIO/S3 연동용)
     */
    public Map<String, Object> parseExcelStyles(InputStream inputStream, Integer sheetIndex) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            return parseWorkbook(workbook, sheetIndex);
        }
    }

    private Map<String, Object> parseWorkbook(XSSFWorkbook workbook, Integer sheetIndex) {
        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

        List<Map<String, Object>> sheetsResult = new ArrayList<>();
        List<String> sheetNames = new ArrayList<>();

        int startSheet = sheetIndex != null ? sheetIndex : 0;
        int endSheet = sheetIndex != null ? sheetIndex + 1 : workbook.getNumberOfSheets();

        for (int si = startSheet; si < endSheet; si++) {
            XSSFSheet sheet = workbook.getSheetAt(si);
            sheetNames.add(sheet.getSheetName());
            sheetsResult.add(parseSheet(sheet, formatter, evaluator));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sheetNames", sheetNames);
        result.put("sheets", sheetsResult);
        result.put("activeSheet", 0);
        return result;
    }

    private Map<String, Object> parseSheet(XSSFSheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        int maxRow = 0, maxCol = 0;

        // 실제 데이터 범위 파악
        for (Row row : sheet) {
            if (row.getRowNum() > maxRow) maxRow = row.getRowNum();
            for (Cell cell : row) {
                if (cell.getColumnIndex() > maxCol) maxCol = cell.getColumnIndex();
            }
        }
        maxRow++; maxCol++;

        // ── 셀 데이터 + 수식 ──
        List<List<String>> data = new ArrayList<>();
        Map<String, String> formulas = new LinkedHashMap<>();

        for (int r = 0; r < maxRow; r++) {
            List<String> rowData = new ArrayList<>();
            Row row = sheet.getRow(r);
            for (int c = 0; c < maxCol; c++) {
                if (row == null) { rowData.add(""); continue; }
                Cell cell = row.getCell(c);
                if (cell == null) { rowData.add(""); continue; }

                // formatted value
                String value;
                try {
                    value = formatter.formatCellValue(cell, evaluator);
                } catch (Exception e) {
                    value = cell.toString();
                }
                rowData.add(value);

                // 수식
                if (cell.getCellType() == CellType.FORMULA) {
                    String ref = org.apache.poi.ss.util.CellReference.convertNumToColString(c) + (r + 1);
                    formulas.put(ref, "=" + cell.getCellFormula());
                }
            }
            data.add(rowData);
        }

        // ── 셀 스타일 ──
        Map<String, Map<String, Object>> styles = new LinkedHashMap<>();

        for (int r = 0; r < maxRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = 0; c < maxCol; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;

                Map<String, Object> style = extractCellStyle(cell);
                if (!style.isEmpty()) {
                    styles.put(r + "-" + c, style);
                }
            }
        }

        // ── 병합 셀 ──
        List<List<Integer>> merges = new ArrayList<>();
        for (CellRangeAddress range : sheet.getMergedRegions()) {
            merges.add(List.of(
                    range.getFirstRow(), range.getFirstColumn(),
                    range.getLastRow() - range.getFirstRow() + 1,
                    range.getLastColumn() - range.getFirstColumn() + 1
            ));
        }

        // ── 열 너비 (px) ──
        List<Integer> colWidths = new ArrayList<>();
        for (int c = 0; c < maxCol; c++) {
            int widthUnits = sheet.getColumnWidth(c); // 1/256 of character width
            int px = (int) Math.round(widthUnits / 256.0 * 7.5);
            colWidths.add(Math.max(px, 20));
        }

        // ── 행 높이 (px) ──
        List<Integer> rowHeights = new ArrayList<>();
        for (int r = 0; r < maxRow; r++) {
            Row row = sheet.getRow(r);
            if (row != null && row.getHeightInPoints() > 0) {
                rowHeights.add(Math.round(row.getHeightInPoints() * 1.333f));
            } else {
                rowHeights.add(20);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", sheet.getSheetName());
        result.put("r", maxRow);
        result.put("c", maxCol);
        result.put("data", data);
        result.put("styles", styles);
        result.put("merges", merges);
        result.put("colWidths", colWidths);
        result.put("rowHeights", rowHeights);
        result.put("formulas", formulas);
        return result;
    }

    /**
     * 셀 스타일 추출
     */
    private Map<String, Object> extractCellStyle(Cell cell) {
        Map<String, Object> style = new LinkedHashMap<>();
        CellStyle cs = cell.getCellStyle();
        if (cs == null) return style;

        // 배경색
        if (cs instanceof XSSFCellStyle xcs) {
            XSSFColor fgColor = xcs.getFillForegroundXSSFColor();
            if (fgColor != null && fgColor.getRGB() != null) {
                byte[] rgb = fgColor.getRGB();
                String hex = String.format("#%02X%02X%02X", rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                if (!"#000000".equals(hex) && !"#FFFFFF".equals(hex)) {
                    style.put("bg", hex);
                }
            }
        }

        // 폰트
        Font font = cell.getSheet().getWorkbook().getFontAt(cs.getFontIndex());
        if (font != null) {
            if (font.getBold()) style.put("b", 1);
            if (font.getItalic()) style.put("i", 1);
            if (font.getFontHeightInPoints() != 11) style.put("fs", font.getFontHeightInPoints());

            if (font instanceof XSSFFont xf) {
                XSSFColor fontColor = xf.getXSSFColor();
                if (fontColor != null && fontColor.getRGB() != null) {
                    byte[] rgb = fontColor.getRGB();
                    String hex = String.format("#%02X%02X%02X", rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                    if (!"#000000".equals(hex)) {
                        style.put("fc", hex);
                    }
                }
            }
        }

        // 정렬
        HorizontalAlignment ha = cs.getAlignment();
        if (ha == HorizontalAlignment.CENTER) style.put("a", "center");
        else if (ha == HorizontalAlignment.RIGHT) style.put("a", "right");
        else if (ha == HorizontalAlignment.LEFT) style.put("a", "left");

        // 테두리
        Map<String, String> borders = new LinkedHashMap<>();
        addBorder(borders, "t", cs.getBorderTop(), cs instanceof XSSFCellStyle xcs ? xcs.getTopBorderXSSFColor() : null);
        addBorder(borders, "b", cs.getBorderBottom(), cs instanceof XSSFCellStyle xcs2 ? xcs2.getBottomBorderXSSFColor() : null);
        addBorder(borders, "l", cs.getBorderLeft(), cs instanceof XSSFCellStyle xcs3 ? xcs3.getLeftBorderXSSFColor() : null);
        addBorder(borders, "r", cs.getBorderRight(), cs instanceof XSSFCellStyle xcs4 ? xcs4.getRightBorderXSSFColor() : null);
        if (!borders.isEmpty()) style.put("bd", borders);

        return style;
    }

    private void addBorder(Map<String, String> borders, String side, BorderStyle bs, XSSFColor color) {
        if (bs == null || bs == BorderStyle.NONE) return;
        String css = switch (bs) {
            case THIN -> "1px solid";
            case MEDIUM -> "2px solid";
            case THICK -> "3px solid";
            case DOUBLE -> "3px double";
            case HAIR -> "1px dotted";
            case DASHED, MEDIUM_DASHED -> "1px dashed";
            case DOTTED -> "1px dotted";
            default -> "1px solid";
        };
        String colorHex = "#000";
        if (color != null && color.getRGB() != null) {
            byte[] rgb = color.getRGB();
            colorHex = String.format("#%02X%02X%02X", rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
        }
        borders.put(side, css + " " + colorHex);
    }
}
