package com.costanalysis.domain.insight.service;

import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@org.springframework.context.annotation.Profile("!mock")
public class ClaudeApiService {

    private final WebClient       claudeWebClient;
    private final ObjectMapper    objectMapper;

    @Value("${claude.model}")
    private String model;

    @Value("${claude.max-tokens:8192}")
    private int maxTokens;

    /**
     * Claude API 스트리밍 호출 → SseEmitter로 실시간 전달
     *
     * @param systemPrompt 시스템 프롬프트
     * @param history      이전 대화 [{role, content}]
     * @param userMessage  현재 사용자 메시지
     * @param useThinking  Extended Thinking 활성화 여부
     * @param emitter      클라이언트 SSE 연결
     */
    public void streamChat(String systemPrompt,
                           List<Map<String, String>> history,
                           String userMessage,
                           boolean useThinking,
                           SseEmitter emitter) {
        try {
            ObjectNode body = buildRequestBody(systemPrompt, history, userMessage, useThinking);

            Flux<String> flux = claudeWebClient.post()
                    .uri("/messages")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .onErrorMap(e -> {
                        log.error("Claude API error: {}", e.getMessage(), e);
                        return new BusinessException(ErrorCode.CLAUDE_API_ERROR);
                    });

            StringBuilder fullText    = new StringBuilder();
            StringBuilder thinkingBuf = new StringBuilder();

            flux.subscribe(
                rawLine -> handleLine(rawLine, fullText, thinkingBuf, emitter),
                error   -> {
                    log.error("SSE stream error: {}", error.getMessage());
                    sendError(emitter, error.getMessage());
                    emitter.complete();
                },
                () -> {
                    try {
                        emitter.send(SseEmitter.event().name("done")
                                .data(Map.of("text", fullText.toString(), "thinking", thinkingBuf.toString())));
                    } catch (IOException e) {
                        log.warn("done event send failed");
                    }
                    emitter.complete();
                }
            );
        } catch (Exception e) {
            log.error("streamChat setup error: {}", e.getMessage(), e);
            sendError(emitter, e.getMessage());
            emitter.complete();
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private ObjectNode buildRequestBody(String systemPrompt,
                                        List<Map<String, String>> history,
                                        String userMessage,
                                        boolean useThinking) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("stream", true);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        if (useThinking) {
            ObjectNode thinking = objectMapper.createObjectNode();
            thinking.put("type", "enabled");
            thinking.put("budget_tokens", 5000);
            body.set("thinking", thinking);
        }

        ArrayNode messages = objectMapper.createArrayNode();
        for (Map<String, String> h : history) {
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", h.get("role"));
            msg.put("content", h.get("content"));
            messages.add(msg);
        }
        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        body.set("messages", messages);

        return body;
    }

    private void handleLine(String rawLine,
                            StringBuilder fullText,
                            StringBuilder thinkingBuf,
                            SseEmitter emitter) {
        if (rawLine == null || rawLine.isBlank()) return;
        // SSE lines: "data: {...}"
        String line = rawLine.startsWith("data: ") ? rawLine.substring(6).trim() : rawLine.trim();
        if (line.equals("[DONE]") || line.isEmpty()) return;

        try {
            JsonNode node = objectMapper.readTree(line);
            String type = node.path("type").asText();

            if ("content_block_delta".equals(type)) {
                JsonNode delta = node.path("delta");
                String deltaType = delta.path("type").asText();

                if ("text_delta".equals(deltaType)) {
                    String text = delta.path("text").asText();
                    fullText.append(text);
                    emitter.send(SseEmitter.event().name("delta").data(Map.of("text", text)));
                } else if ("thinking_delta".equals(deltaType)) {
                    String thinking = delta.path("thinking").asText();
                    thinkingBuf.append(thinking);
                    emitter.send(SseEmitter.event().name("thinking").data(Map.of("thinking", thinking)));
                }
            }
        } catch (Exception e) {
            log.debug("SSE line parse skip: {}", rawLine);
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
        } catch (IOException ignored) {}
    }
}
