package com.costanalysis.domain.notification.controller;

import com.costanalysis.domain.notification.dto.NotificationResponse;
import com.costanalysis.domain.notification.service.NotificationService;
import com.costanalysis.global.response.ApiResponse;
import com.costanalysis.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtTokenProvider    jwtTokenProvider;

    @Operation(summary = "알림 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> list(
            HttpServletRequest req,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.list(userId(req), pageable)));
    }

    @Operation(summary = "읽지 않은 알림 수")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(notificationService.unreadCount(userId(req))));
    }

    @Operation(summary = "전체 읽음 처리")
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(HttpServletRequest req) {
        notificationService.markAllRead(userId(req));
        return ResponseEntity.ok(ApiResponse.ok("모두 읽음 처리 되었습니다."));
    }

    @Operation(summary = "개별 읽음 처리")
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(ApiResponse.ok("읽음 처리 되었습니다."));
    }

    private Long userId(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        String token  = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : "";
        return jwtTokenProvider.getUserId(token);
    }
}
