package com.costanalysis.global.mock;

import com.costanalysis.domain.quotation.dto.ParsingNoteCreateDto;
import com.costanalysis.domain.quotation.dto.ParsingNoteResponse;
import com.costanalysis.domain.quotation.service.ParsingNoteService;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock 프로파일 전용 파싱 노트 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockParsingNoteService extends ParsingNoteService {

    private final MockDataStore dataStore;

    public MockParsingNoteService(MockDataStore dataStore) {
        super(null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public List<ParsingNoteResponse> listByFile(Long fileId) {
        return dataStore.getNotes().values().stream()
                .filter(n -> fileId.equals(n.get("fileId")))
                .sorted(Comparator.comparing((Map<String, Object> n) -> (OffsetDateTime) n.get("createdAt")).reversed())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ParsingNoteResponse create(Long userId, ParsingNoteCreateDto dto) {
        Long id = dataStore.nextNoteId();
        Map<String, Object> note = new java.util.concurrent.ConcurrentHashMap<>();
        note.put("id", id);
        note.put("fileId", dto.getFileId());
        note.put("userId", userId);
        note.put("type", dto.getType() != null ? dto.getType() : "parsing");
        note.put("content", dto.getContent());
        note.put("createdAt", OffsetDateTime.now());
        dataStore.getNotes().put(id, note);
        log.debug("[MOCK] 노트 생성: id={}, fileId={}", id, dto.getFileId());
        return toResponse(note);
    }

    @Override
    public void delete(Long noteId, Long userId) {
        Map<String, Object> note = dataStore.getNotes().get(noteId);
        if (note == null) throw new BusinessException(ErrorCode.NOTE_NOT_FOUND);
        if (!userId.equals(note.get("userId"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        dataStore.getNotes().remove(noteId);
        log.debug("[MOCK] 노트 삭제: id={}", noteId);
    }

    @Override
    public long countByFile(Long fileId) {
        return dataStore.getNotes().values().stream()
                .filter(n -> fileId.equals(n.get("fileId")))
                .count();
    }

    private ParsingNoteResponse toResponse(Map<String, Object> n) {
        return ParsingNoteResponse.builder()
                .id((Long) n.get("id"))
                .fileId((Long) n.get("fileId"))
                .userId((Long) n.get("userId"))
                .type((String) n.get("type"))
                .content((String) n.get("content"))
                .createdAt((OffsetDateTime) n.get("createdAt"))
                .build();
    }
}
