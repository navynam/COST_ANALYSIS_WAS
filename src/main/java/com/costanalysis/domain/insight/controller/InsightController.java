package com.costanalysis.domain.insight.controller;

import com.costanalysis.domain.insight.dto.*;
import com.costanalysis.domain.insight.service.InsightService;
import com.costanalysis.global.response.ApiResponse;
import com.costanalysis.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Tag(name = "Insight", description = "AI 인사이트 채팅 API")
@RestController
@RequestMapping("/api/v1/insight")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class InsightController {

    private final InsightService   insightService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "채팅 세션 생성")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<InsightSessionResponse>> createSession(
            @RequestBody SessionCreateRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(ApiResponse.ok(insightService.createSession(userId(httpReq), req)));
    }

    @Operation(summary = "내 채팅 세션 목록")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Page<InsightSessionResponse>>> listSessions(
            HttpServletRequest httpReq,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(insightService.listSessions(userId(httpReq), pageable)));
    }

    @Operation(summary = "채팅 세션 메시지 목록")
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<InsightMessageResponse>>> getMessages(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(insightService.getMessages(sessionId)));
    }

    @Operation(summary = "채팅 세션 삭제")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable Long sessionId) {
        insightService.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.ok("세션이 삭제되었습니다."));
    }

    /**
     * Claude API SSE 스트리밍 채팅.
     * 클라이언트는 EventSource 로 연결하고, delta / thinking / done / error 이벤트를 수신.
     * done 이벤트의 text 값을 받아 assistant 메시지로 저장하는 후속 호출은 saveMessage 로 처리.
     */
    @Operation(summary = "AI 채팅 (SSE 스트리밍)")
    @PostMapping(value = "/sessions/{sessionId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @PathVariable Long sessionId,
            @Valid @RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(3 * 60 * 1000L); // 3분
        insightService.chat(sessionId, req, emitter);
        return emitter;
    }

    @Operation(summary = "어시스턴트 메시지 저장 (done 이벤트 수신 후 호출)")
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<Void>> saveMessage(
            @PathVariable Long sessionId,
            @RequestBody InsightMessageResponse body) {
        insightService.saveAssistantMessage(sessionId, body.getContent(), body.getThinking());
        return ResponseEntity.ok(ApiResponse.ok("저장되었습니다."));
    }

    private Long userId(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        String token  = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : "";
        return jwtTokenProvider.getUserId(token);
    }
}
