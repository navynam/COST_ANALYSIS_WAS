package com.costanalysis.domain.quotation.service;

import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.entity.Quotation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@org.springframework.context.annotation.Profile("!mock")
public class ExcelParserService {

    public List<ParsedItem> parse(InputStream is, Quotation quotation) throws Exception {
        List<ParsedItem> items = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            int headerRow = findHeaderRow(sheet);
            if (headerRow < 0) headerRow = 0;

            for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row)) continue;

                ParsedItem item = ParsedItem.builder()
                        .quotation(quotation)
                        .rowIndex(r)
                        .itemCode(getString(row, 0))
                        .itemName(getString(row, 1))
                        .specification(getString(row, 2))
                        .unit(getString(row, 3))
                        .quantity(getInt(row, 4))
                        .unitPrice(getDecimal(row, 5))
                        .totalPrice(getDecimal(row, 6))
                        .category(getString(row, 7))
                        .notes(getString(row, 8))
                        .confidenceScore(BigDecimal.valueOf(0.90))
                        .build();
                items.add(item);
            }
        }
        return items;
    }

    private int findHeaderRow(Sheet sheet) {
        for (int r = 0; r <= Math.min(5, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String first = getString(row, 0);
            if (first != null && (first.contains("품목") || first.contains("코드") || first.contains("번호"))) {
                return r;
            }
        }
        return -1;
    }

    private boolean isEmptyRow(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private String getString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private Integer getInt(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        return (int) cell.getNumericCellValue();
    }

    private BigDecimal getDecimal(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        return BigDecimal.valueOf(cell.getNumericCellValue());
    }
}
