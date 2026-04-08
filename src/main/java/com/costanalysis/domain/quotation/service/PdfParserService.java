package com.costanalysis.domain.quotation.service;

import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.entity.Quotation;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@org.springframework.context.annotation.Profile("!mock")
public class PdfParserService {

    // 간단한 패턴 매처: 숫자로 시작하는 행을 품목으로 간주
    // 예: "1  CPU i7-13700K  EA  1  450,000  450,000"
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^(\\d+)\\s+(.+?)\\s+(EA|개|식|SET|BOX|M|KG|L)\\s+(\\d+)\\s+([\\d,]+)\\s+([\\d,]+)",
            Pattern.CASE_INSENSITIVE
    );

    public List<ParsedItem> parse(InputStream is, Quotation quotation) throws Exception {
        List<ParsedItem> items = new ArrayList<>();
        byte[] bytes = is.readAllBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            String[] lines = text.split("\\n");
            int rowIdx = 0;
            for (String line : lines) {
                line = line.trim();
                Matcher m = LINE_PATTERN.matcher(line);
                if (m.find()) {
                    ParsedItem item = ParsedItem.builder()
                            .quotation(quotation)
                            .rowIndex(rowIdx++)
                            .itemName(m.group(2).trim())
                            .unit(m.group(3).trim())
                            .quantity(Integer.parseInt(m.group(4)))
                            .unitPrice(parseAmount(m.group(5)))
                            .totalPrice(parseAmount(m.group(6)))
                            .confidenceScore(BigDecimal.valueOf(0.75))
                            .build();
                    items.add(item);
                }
            }
        }
        return items;
    }

    private BigDecimal parseAmount(String val) {
        try {
            return new BigDecimal(val.replace(",", ""));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
