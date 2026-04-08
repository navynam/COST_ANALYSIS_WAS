package com.costanalysis.domain.insight.service;

import com.costanalysis.domain.insight.dto.*;
import com.costanalysis.domain.insight.entity.InsightMessage;
import com.costanalysis.domain.insight.entity.InsightSession;
import com.costanalysis.domain.insight.repository.InsightMessageRepository;
import com.costanalysis.domain.insight.repository.InsightSessionRepository;
import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.entity.Quotation;
import com.costanalysis.domain.quotation.repository.ParsedItemRepository;
import com.costanalysis.domain.quotation.repository.QuotationRepository;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.domain.user.repository.UserRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@org.springframework.context.annotation.Profile("!mock")
public class InsightService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 견적서 분석 전문 AI 어시스턴트입니다.
            사용자의 견적서 데이터를 바탕으로 비용 최적화, 이상값 탐지, 공급업체 비교 등의 인사이트를 제공합니다.
            답변은 항상 한국어로 하고, 구체적인 수치와 근거를 포함하세요.

            %s
            """;

    private final InsightSessionRepository  sessionRepository;
    private final InsightMessageRepository  messageRepository;
    private final QuotationRepository       quotationRepository;
    private final ParsedItemRepository      parsedItemRepository;
    private final UserRepository            userRepository;
    private final ClaudeApiService          claudeApiService;

    @Transactional
    public InsightSessionResponse createSession(Long userId, SessionCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        InsightSession session = InsightSession.builder()
                .user(user)
                .title(req.getTitle() != null ? req.getTitle() : "새 대화")
                .build();

        if (req.getQuotationId() != null) {
            Quotation q = quotationRepository.findById(req.getQuotationId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUOTATION_NOT_FOUND));
            session.setQuotation(q);
        }

        return InsightSessionResponse.from(sessionRepository.save(session));
    }

    public Page<InsightSessionResponse> listSessions(Long userId, Pageable pageable) {
        return sessionRepository.findByUser_IdOrderByUpdatedAtDesc(userId, pageable)
                .map(InsightSessionResponse::from);
    }

    public List<InsightMessageResponse> getMessages(Long sessionId) {
        return messageRepository.findBySession_IdOrderByCreatedAt(sessionId)
                .stream().map(InsightMessageResponse::from).toList();
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        InsightSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        sessionRepository.delete(session);
    }

    /**
     * 사용자 메시지를 저장하고 Claude API SSE 스트리밍을 시작한다.
     * 응답 완료 후 assistant 메시지를 DB에 저장하는 것은 InsightController에서 처리.
     */
    @Transactional
    public void chat(Long sessionId, ChatRequest req, SseEmitter emitter) {
        InsightSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        // 사용자 메시지 저장
        InsightMessage userMsg = InsightMessage.builder()
                .session(session)
                .role("user")
                .content(req.getMessage())
                .build();
        messageRepository.save(userMsg);

        // 대화 히스토리 구성
        List<Map<String, String>> history = messageRepository
                .findBySession_IdOrderByCreatedAt(sessionId)
                .stream()
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());

        // 시스템 프롬프트 (견적서 컨텍스트 포함)
        String context = buildQuotationContext(session);
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, context);

        // Claude 스트리밍 — 완료 후 emitter에 done 이벤트 전송 (ClaudeApiService 내부 처리)
        // 응답 저장은 done 이벤트를 받은 InsightController에서 수행
        claudeApiService.streamChat(systemPrompt, history, req.getMessage(), req.isUseThinking(), emitter);
    }

    @Transactional
    public void saveAssistantMessage(Long sessionId, String content, String thinking) {
        InsightSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        InsightMessage msg = InsightMessage.builder()
                .session(session)
                .role("assistant")
                .content(content)
                .thinking(thinking)
                .build();
        messageRepository.save(msg);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private String buildQuotationContext(InsightSession session) {
        if (session.getQuotation() == null) return "";
        Quotation q = session.getQuotation();
        List<ParsedItem> items = parsedItemRepository.findByQuotation_IdOrderByRowIndex(q.getId());
        BigDecimal total = items.stream()
                .filter(i -> i.getTotalPrice() != null)
                .map(ParsedItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format("""
                [견적서 컨텍스트]
                - 파일명: %s
                - 총 항목 수: %d
                - 총 금액: %s원
                - 상위 항목: %s
                """,
                q.getOriginalFilename(),
                items.size(),
                total.setScale(0, RoundingMode.HALF_UP).toPlainString(),
                items.stream().limit(5)
                        .map(i -> i.getItemName() + "(" + (i.getTotalPrice() != null ? i.getTotalPrice().setScale(0, RoundingMode.HALF_UP) : "?") + "원)")
                        .collect(Collectors.joining(", "))
        );
    }
}
