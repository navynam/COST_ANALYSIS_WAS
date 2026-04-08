package com.costanalysis.domain.quotation.service;

import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.entity.Quotation;
import com.costanalysis.domain.quotation.repository.ParsedItemRepository;
import com.costanalysis.domain.quotation.repository.QuotationRepository;
import com.costanalysis.global.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!mock")
public class QuotationParsingService {

    private final QuotationRepository  quotationRepository;
    private final ParsedItemRepository parsedItemRepository;
    private final ExcelParserService   excelParser;
    private final PdfParserService     pdfParser;
    private final FileStorageService   fileStorage;

    @Async
    public void parseAsync(Long quotationId, SseEmitter emitter) {
        try {
            Quotation q = quotationRepository.findById(quotationId)
                    .orElseThrow(() -> new RuntimeException("Quotation not found: " + quotationId));

            sendEvent(emitter, "progress", Map.of("step", "DOWNLOADING", "pct", 10));
            q.setStatus("PARSING");
            quotationRepository.save(q);

            try (InputStream is = fileStorage.download(q.getStoragePath())) {
                sendEvent(emitter, "progress", Map.of("step", "PARSING", "pct", 40));

                List<ParsedItem> items = switch (q.getFileType()) {
                    case "XLSX", "XLS" -> excelParser.parse(is, q);
                    case "PDF"         -> pdfParser.parse(is, q);
                    default            -> throw new RuntimeException("Unsupported file type: " + q.getFileType());
                };

                sendEvent(emitter, "progress", Map.of("step", "SAVING", "pct", 80));
                parsedItemRepository.deleteByQuotation_Id(quotationId);
                parsedItemRepository.saveAll(items);

                q.setStatus("PARSED");
                q.setTotalItems(items.size());
                quotationRepository.save(q);

                sendEvent(emitter, "done", Map.of(
                        "step", "DONE",
                        "pct", 100,
                        "totalItems", items.size()
                ));
            }
        } catch (Exception e) {
            log.error("Parsing failed for quotation {}: {}", quotationId, e.getMessage(), e);
            try {
                quotationRepository.findById(quotationId).ifPresent(q -> {
                    q.setStatus("FAILED");
                    q.setParseErrorMessage(e.getMessage());
                    quotationRepository.save(q);
                });
                sendEvent(emitter, "error", Map.of("message", e.getMessage()));
            } catch (Exception ignored) {}
        } finally {
            emitter.complete();
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.warn("SSE send failed: {}", e.getMessage());
        }
    }
}
