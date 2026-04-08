package com.costanalysis.domain.user.controller;

import com.costanalysis.domain.user.dto.*;
import com.costanalysis.domain.user.service.UserService;
import com.costanalysis.global.response.ApiResponse;
import com.costanalysis.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService      userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(HttpServletRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfile(extractUserId(req))));
    }

    @Operation(summary = "내 프로필 수정")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            HttpServletRequest req,
            @Valid @RequestBody UserProfileRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateProfile(extractUserId(req), body)));
    }

    @Operation(summary = "비밀번호 변경")
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            HttpServletRequest req,
            @Valid @RequestBody PasswordChangeRequest body) {
        userService.changePassword(extractUserId(req), body);
        return ResponseEntity.ok(ApiResponse.ok("비밀번호가 변경되었습니다."));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @Operation(summary = "전체 사용자 목록 (ADMIN)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.listUsers()));
    }

    @Operation(summary = "사용자 생성 (ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUser(
            @Valid @RequestBody UserCreateRequest body) {
        return ResponseEntity.ok(ApiResponse.ok(userService.createUser(body)));
    }

    @Operation(summary = "계정 활성화/비활성화 (ADMIN)")
    @PutMapping("/{userId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleActive(
            @PathVariable Long userId,
            @RequestParam boolean active) {
        userService.toggleActive(userId, active);
        return ResponseEntity.ok(ApiResponse.ok("계정 상태가 변경되었습니다."));
    }

    private Long extractUserId(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        String token  = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : "";
        return jwtTokenProvider.getUserId(token);
    }
}
