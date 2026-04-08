package com.costanalysis.domain.quotation.repository;

import com.costanalysis.domain.quotation.entity.ParsingNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParsingNoteRepository extends JpaRepository<ParsingNote, Long> {

    List<ParsingNote> findByFileIdOrderByCreatedAtDesc(Long fileId);

    List<ParsingNote> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByFileId(Long fileId);
}
