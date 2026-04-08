package com.costanalysis.global.mock;

import com.costanalysis.domain.insight.dto.*;
import com.costanalysis.domain.insight.service.InsightService;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Mock 프로파일 전용 인사이트 서비스.
 * Claude API 대신 고정 응답을 반환한다.
 */
@Slf4j
@Service
@Profile("mock")
public class MockInsightService extends InsightService {

    private final MockDataStore dataStore;
    private final ConcurrentHashMap<Long, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, List<InsightMessageResponse>> messages = new ConcurrentHashMap<>();
    private final AtomicLong sessionSeq = new AtomicLong(0);
    private final AtomicLong messageSeq = new AtomicLong(0);

    public MockInsightService(MockDataStore dataStore) {
        super(null, null, null, null, null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public InsightSessionResponse createSession(Long userId, SessionCreateRequest req) {
        User user = dataStore.getUsers().get(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        Long id = sessionSeq.incrementAndGet();
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> session = new ConcurrentHashMap<>();
        session.put("id", id);
        session.put("userId", userId);
        session.put("title", req.getTitle() != null ? req.getTitle() : "새 대화");
        session.put("quotationId", req.getQuotationId());
        session.put("createdAt", now);
        session.put("updatedAt", now);
        sessions.put(id, session);
        messages.put(id, Collections.synchronizedList(new ArrayList<>()));
        log.debug("[MOCK] 인사이트 세션 생성: id={}", id);

        return InsightSessionResponse.builder()
                .id(id)
                .title((String) session.get("title"))
                .quotationId(req.getQuotationId())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Override
    public Page<InsightSessionResponse> listSessions(Long userId, Pageable pageable) {
        List<InsightSessionResponse> filtered = sessions.values().stream()
                .filter(s -> userId.equals(s.get("userId")))
                .sorted(Comparator.comparing((Map<String, Object> s) -> (OffsetDateTime) s.get("updatedAt")).reversed())
                .map(this::toSessionResponse)
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<InsightSessionResponse> sub = start >= filtered.size() ? Collections.emptyList() : filtered.subList(start, end);
        return new PageImpl<>(sub, pageable, filtered.size());
    }

    @Override
    public List<InsightMessageResponse> getMessages(Long sessionId) {
        return messages.getOrDefault(sessionId, Collections.emptyList());
    }

    @Override
    public void deleteSession(Long sessionId) {
        if (!sessions.containsKey(sessionId)) throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        sessions.remove(sessionId);
        messages.remove(sessionId);
        log.debug("[MOCK] 인사이트 세션 삭제: id={}", sessionId);
    }

    @Override
    public void chat(Long sessionId, ChatRequest req, SseEmitter emitter) {
        Map<String, Object> session = sessions.get(sessionId);
        if (session == null) throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);

        // 사용자 메시지 저장
        Long userMsgId = messageSeq.incrementAndGet();
        messages.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(InsightMessageResponse.builder()
                        .id(userMsgId).role("user").content(req.getMessage()).createdAt(OffsetDateTime.now())
                        .build());

        // Mock 응답 (별도 스레드)
        String mockResponse = "[Mock 응답] 견적서 분석 결과를 안내드립니다. 전체적으로 재료비 비중이 높으며, 일부 항목에서 단가 편차가 관찰됩니다.";
        new Thread(() -> {
            try {
                // 글자 단위로 스트리밍 시뮬레이션
                for (int i = 0; i < mockResponse.length(); i++) {
                    String ch = String.valueOf(mockResponse.charAt(i));
                    emitter.send(SseEmitter.event().name("delta").data(Map.of("text", ch)));
                    Thread.sleep(20);
                }
                emitter.send(SseEmitter.event().name("done").data(Map.of("text", mockResponse, "thinking", "")));

                // assistant 메시지 저장
                Long asstMsgId = messageSeq.incrementAndGet();
                messages.get(sessionId).add(InsightMessageResponse.builder()
                        .id(asstMsgId).role("assistant").content(mockResponse).createdAt(OffsetDateTime.now())
                        .build());
            } catch (IOException | InterruptedException e) {
                log.warn("[MOCK] 인사이트 SSE 오류: {}", e.getMessage());
                Thread.currentThread().interrupt();
            } finally {
                emitter.complete();
            }
        }).start();
    }

    @Override
    public void saveAssistantMessage(Long sessionId, String content, String thinking) {
        Long id = messageSeq.incrementAndGet();
        messages.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(InsightMessageResponse.builder()
                        .id(id).role("assistant").content(content).thinking(thinking).createdAt(OffsetDateTime.now())
                        .build());
        log.debug("[MOCK] 어시스턴트 메시지 저장: sessionId={}", sessionId);
    }

    private InsightSessionResponse toSessionResponse(Map<String, Object> s) {
        return InsightSessionResponse.builder()
                .id((Long) s.get("id"))
                .title((String) s.get("title"))
                .quotationId((Long) s.get("quotationId"))
                .createdAt((OffsetDateTime) s.get("createdAt"))
                .updatedAt((OffsetDateTime) s.get("updatedAt"))
                .build();
    }
}
