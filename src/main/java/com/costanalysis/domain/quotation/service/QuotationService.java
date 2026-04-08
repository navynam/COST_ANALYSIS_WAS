package com.costanalysis.domain.quotation.service;

import com.costanalysis.domain.quotation.dto.QuotationDetail;
import com.costanalysis.domain.quotation.dto.QuotationSummary;
import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.entity.Quotation;
import com.costanalysis.domain.quotation.repository.ParsedItemRepository;
import com.costanalysis.domain.quotation.repository.QuotationRepository;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.domain.user.repository.UserRepository;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import com.costanalysis.global.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@org.springframework.context.annotation.Profile("!mock")
public class QuotationService {

    private static final Set<String> ALLOWED_TYPES = Set.of("XLSX", "XLS", "PDF");
    private static final long MAX_SIZE = 50L * 1024 * 1024; // 50 MB

    private final QuotationRepository  quotationRepository;
    private final ParsedItemRepository parsedItemRepository;
    private final UserRepository       userRepository;
    private final FileStorageService   fileStorage;

    @Transactional
    public Quotation upload(Long userId, MultipartFile file) {
        validateFile(file);
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String ext         = getExtension(file.getOriginalFilename()).toUpperCase();
        String storagePath = fileStorage.upload(file, "quotations/" + userId);

        Quotation q = Quotation.builder()
                .uploadedBy(uploader)
                .originalFilename(file.getOriginalFilename())
                .storagePath(storagePath)
                .fileType(ext)
                .fileSize(file.getSize())
                .status("UPLOADED")
                .build();
        return quotationRepository.save(q);
    }

    public Page<QuotationSummary> listMine(Long userId, Pageable pageable) {
        return quotationRepository
                .findByUploadedBy_IdOrderByCreatedAtDesc(userId, pageable)
                .map(QuotationSummary::from);
    }

    public Page<QuotationSummary> listAll(Pageable pageable) {
        return quotationRepository.findAllByOrderByCreatedAtDesc(pageable).map(QuotationSummary::from);
    }

    public QuotationDetail getDetail(Long id) {
        Quotation q = quotationRepository.findByIdWithItems(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTATION_NOT_FOUND));
        String url = fileStorage.getPresignedUrl(q.getStoragePath());
        return QuotationDetail.from(q, url);
    }

    @Transactional
    public void delete(Long id, Long userId, boolean isAdmin) {
        Quotation q = quotationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTATION_NOT_FOUND));
        if (!isAdmin && !q.getUploadedBy().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        fileStorage.delete(q.getStoragePath());
        quotationRepository.delete(q);
    }

    @Transactional
    public ParsedItem updateItem(Long itemId, ParsedItem patch) {
        ParsedItem item = parsedItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUOTATION_NOT_FOUND));
        if (patch.getItemName()      != null) item.setItemName(patch.getItemName());
        if (patch.getSpecification() != null) item.setSpecification(patch.getSpecification());
        if (patch.getUnit()          != null) item.setUnit(patch.getUnit());
        if (patch.getQuantity()      != null) item.setQuantity(patch.getQuantity());
        if (patch.getUnitPrice()     != null) item.setUnitPrice(patch.getUnitPrice());
        if (patch.getTotalPrice()    != null) item.setTotalPrice(patch.getTotalPrice());
        if (patch.getCategory()      != null) item.setCategory(patch.getCategory());
        item.setVerified(patch.isVerified());
        return parsedItemRepository.save(item);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file.getSize() > MAX_SIZE) throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        String ext = getExtension(file.getOriginalFilename()).toUpperCase();
        if (!ALLOWED_TYPES.contains(ext)) throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    private String getExtension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx + 1) : "";
    }
}
