package com.costanalysis.domain.quotation.dto;

import com.costanalysis.domain.quotation.entity.ParsingNote;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter @Builder
public class ParsingNoteResponse {

    private Long id;
    private Long fileId;
    private Long userId;
    private String type;
    private String content;
    private OffsetDateTime createdAt;

    public static ParsingNoteResponse from(ParsingNote note) {
        return ParsingNoteResponse.builder()
                .id(note.getId())
                .fileId(note.getFileId())
                .userId(note.getUserId())
                .type(note.getType())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .build();
    }
}
