package com.costanalysis.domain.quotation.service;

import com.costanalysis.domain.quotation.dto.ParsingNoteCreateDto;
import com.costanalysis.domain.quotation.dto.ParsingNoteResponse;
import com.costanalysis.domain.quotation.entity.ParsingNote;
import com.costanalysis.domain.quotation.repository.ParsingNoteRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@org.springframework.context.annotation.Profile("!mock")
public class ParsingNoteService {

    private final ParsingNoteRepository noteRepository;

    /** 특정 파일의 노트 목록 */
    public List<ParsingNoteResponse> listByFile(Long fileId) {
        return noteRepository.findByFileIdOrderByCreatedAtDesc(fileId)
                .stream().map(ParsingNoteResponse::from).toList();
    }

    /** 노트 생성 */
    @Transactional
    public ParsingNoteResponse create(Long userId, ParsingNoteCreateDto dto) {
        ParsingNote note = ParsingNote.builder()
                .fileId(dto.getFileId())
                .userId(userId)
                .type(dto.getType() != null ? dto.getType() : "parsing")
                .content(dto.getContent())
                .build();
        return ParsingNoteResponse.from(noteRepository.save(note));
    }

    /** 노트 삭제 (본인 작성분만) */
    @Transactional
    public void delete(Long noteId, Long userId) {
        ParsingNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTE_NOT_FOUND));
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        noteRepository.delete(note);
    }

    /** 파일별 노트 개수 */
    public long countByFile(Long fileId) {
        return noteRepository.countByFileId(fileId);
    }
}
