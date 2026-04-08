package com.costanalysis.domain.quotation.controller;

import com.costanalysis.domain.quotation.dto.ParsingNoteCreateDto;
import com.costanalysis.domain.quotation.dto.ParsingNoteResponse;
import com.costanalysis.domain.quotation.service.ParsingNoteService;
import com.costanalysis.global.response.ApiResponse;
import com.costanalysis.global.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ParsingNote", description = "파싱 노트 API")
@RestController
@RequestMapping("/api/v1/quotations/notes")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class ParsingNoteController {

    private final ParsingNoteService noteService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "특정 파일의 노트 목록")
    @GetMapping("/file/{fileId}")
    public ResponseEntity<ApiResponse<List<ParsingNoteResponse>>> listByFile(
            @PathVariable Long fileId) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.listByFile(fileId)));
    }

    @Operation(summary = "노트 작성")
    @PostMapping
    public ResponseEntity<ApiResponse<ParsingNoteResponse>> create(
            HttpServletRequest req,
            @Valid @RequestBody ParsingNoteCreateDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.create(userId(req), dto)));
    }

    @Operation(summary = "노트 삭제 (본인 작성분만)")
    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long noteId,
            HttpServletRequest req) {
        noteService.delete(noteId, userId(req));
        return ResponseEntity.ok(ApiResponse.ok("노트가 삭제되었습니다."));
    }

    @Operation(summary = "파일별 노트 개수")
    @GetMapping("/file/{fileId}/count")
    public ResponseEntity<ApiResponse<Long>> countByFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(ApiResponse.ok(noteService.countByFile(fileId)));
    }

    private Long userId(HttpServletRequest req) {
        String bearer = req.getHeader("Authorization");
        String token = (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : "";
        return jwtTokenProvider.getUserId(token);
    }
}
