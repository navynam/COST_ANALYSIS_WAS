package com.costanalysis.global.mock;

import com.costanalysis.domain.quotation.service.QuotationParsingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * Mock 프로파일 전용 파싱 서비스.
 * SSE로 가짜 진행률을 전송한다.
 */
@Slf4j
@Service
@Profile("mock")
public class MockQuotationParsingService extends QuotationParsingService {

    private final MockDataStore dataStore;

    public MockQuotationParsingService(MockDataStore dataStore) {
        super(null, null, null, null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public void parseAsync(Long quotationId, SseEmitter emitter) {
        log.debug("[MOCK] 파싱 시작: quotationId={}", quotationId);

        // 별도 스레드에서 Mock SSE 이벤트 전송
        new Thread(() -> {
            try {
                sendEvent(emitter, "progress", Map.of("step", "DOWNLOADING", "pct", 10));
                Thread.sleep(500);
                sendEvent(emitter, "progress", Map.of("step", "PARSING", "pct", 40));
                Thread.sleep(500);
                sendEvent(emitter, "progress", Map.of("step", "SAVING", "pct", 80));
                Thread.sleep(500);

                // Mock 데이터 업데이트
                Map<String, Object> q = dataStore.getQuotations().get(quotationId);
                if (q != null) {
                    q.put("status", "verifying");
                    q.put("progress", 100);
                    q.put("parsedItems", 15);
                    q.put("anomalies", 1);
                }

                sendEvent(emitter, "done", Map.of("step", "DONE", "pct", 100, "totalItems", 15));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                emitter.complete();
            }
        }).start();
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.warn("[MOCK] SSE 전송 실패: {}", e.getMessage());
        }
    }
}
