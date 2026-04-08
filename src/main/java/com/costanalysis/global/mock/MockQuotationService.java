package com.costanalysis.global.mock;

import com.costanalysis.domain.quotation.dto.QuotationDetail;
import com.costanalysis.domain.quotation.dto.QuotationSummary;
import com.costanalysis.domain.quotation.entity.ParsedItem;
import com.costanalysis.domain.quotation.entity.Quotation;
import com.costanalysis.domain.quotation.service.QuotationService;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock 프로파일 전용 견적서 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockQuotationService extends QuotationService {

    private final MockDataStore dataStore;

    public MockQuotationService(MockDataStore dataStore) {
        super(null, null, null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public Quotation upload(Long userId, MultipartFile file) {
        log.debug("[MOCK] 파일 업로드: {}", file.getOriginalFilename());
        Long id = dataStore.nextQuotationId();
        Map<String, Object> q = new ConcurrentHashMapHelper();
        q.put("id", id);
        q.put("name", file.getOriginalFilename());
        q.put("status", "extracting");
        q.put("progress", 0);
        // ConcurrentHashMap does not allow null values; omit null fields
        q.put("uploadDate", java.time.LocalDate.now().toString());
        q.put("fileSize", formatFileSize(file.getSize()));
        q.put("sheets", 1);
        q.put("uploader", dataStore.getUsers().getOrDefault(userId, dataStore.getUsers().get(1L)).getName());
        q.put("department", dataStore.getUsers().getOrDefault(userId, dataStore.getUsers().get(1L)).getDepartment());
        q.put("fileType", getExtension(file.getOriginalFilename()).toUpperCase());
        q.put("createdAt", OffsetDateTime.now());
        dataStore.getQuotations().put(id, q);

        // Quotation 엔티티 반환 (ID만 있으면 됨)
        return Quotation.builder().id(id).build();
    }

    @Override
    public Page<QuotationSummary> listMine(Long userId, Pageable pageable) {
        String userName = dataStore.getUsers().containsKey(userId)
                ? dataStore.getUsers().get(userId).getName() : "김관리";
        List<QuotationSummary> filtered = dataStore.getQuotations().values().stream()
                .filter(q -> userName.equals(q.get("uploader")))
                .sorted(Comparator.comparing((Map<String, Object> q) -> (OffsetDateTime) q.get("createdAt")).reversed())
                .map(this::toSummary)
                .collect(Collectors.toList());
        return paginate(filtered, pageable);
    }

    @Override
    public Page<QuotationSummary> listAll(Pageable pageable) {
        List<QuotationSummary> all = dataStore.getQuotations().values().stream()
                .sorted(Comparator.comparing((Map<String, Object> q) -> (OffsetDateTime) q.get("createdAt")).reversed())
                .map(this::toSummary)
                .collect(Collectors.toList());
        return paginate(all, pageable);
    }

    @Override
    public QuotationDetail getDetail(Long id) {
        Map<String, Object> q = dataStore.getQuotations().get(id);
        if (q == null) throw new BusinessException(ErrorCode.QUOTATION_NOT_FOUND);

        return QuotationDetail.builder()
                .id((Long) q.get("id"))
                .originalFilename((String) q.get("name"))
                .fileType((String) q.get("fileType"))
                .fileSize(parseFileSize((String) q.get("fileSize")))
                .status(mapStatus((String) q.get("status")))
                .uploaderName((String) q.get("uploader"))
                .createdAt((OffsetDateTime) q.get("createdAt"))
                .totalItems((Integer) q.get("parsedItems"))
                .downloadUrl("mock://download/" + id)
                .items(Collections.emptyList())
                .build();
    }

    @Override
    public void delete(Long id, Long userId, boolean isAdmin) {
        if (!dataStore.getQuotations().containsKey(id)) {
            throw new BusinessException(ErrorCode.QUOTATION_NOT_FOUND);
        }
        dataStore.getQuotations().remove(id);
        log.debug("[MOCK] 견적서 삭제: {}", id);
    }

    @Override
    public ParsedItem updateItem(Long itemId, ParsedItem patch) {
        log.debug("[MOCK] 파싱 항목 수정: {}", itemId);
        return patch; // mock에서는 그대로 반환
    }

    // ── 헬퍼 ──

    private QuotationSummary toSummary(Map<String, Object> q) {
        return QuotationSummary.builder()
                .id((Long) q.get("id"))
                .originalFilename((String) q.get("name"))
                .fileType((String) q.get("fileType"))
                .fileSize(parseFileSize((String) q.get("fileSize")))
                .status(mapStatus((String) q.get("status")))
                .totalItems((Integer) q.get("parsedItems"))
                .uploaderName((String) q.get("uploader"))
                .createdAt((OffsetDateTime) q.get("createdAt"))
                .build();
    }

    private String mapStatus(String frontendStatus) {
        return switch (frontendStatus) {
            case "extracting" -> "PARSING";
            case "verifying"  -> "PARSED";
            case "verified"   -> "VERIFIED";
            case "analyzing"  -> "PARSED";
            case "analyzed"   -> "ANALYZED";
            case "failed"     -> "FAILED";
            default           -> frontendStatus.toUpperCase();
        };
    }

    private <T> Page<T> paginate(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<T> sub = start >= list.size() ? Collections.emptyList() : list.subList(start, end);
        return new PageImpl<>(sub, pageable, list.size());
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private Long parseFileSize(String sizeStr) {
        if (sizeStr == null) return 0L;
        try {
            String[] parts = sizeStr.split(" ");
            double val = Double.parseDouble(parts[0]);
            if (parts.length > 1) {
                return switch (parts[1]) {
                    case "KB" -> (long) (val * 1024);
                    case "MB" -> (long) (val * 1024 * 1024);
                    case "GB" -> (long) (val * 1024 * 1024 * 1024);
                    default -> (long) val;
                };
            }
            return (long) val;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String getExtension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx + 1) : "";
    }

    // ConcurrentHashMap 팩토리
    private static class ConcurrentHashMapHelper extends java.util.concurrent.ConcurrentHashMap<String, Object> {}
}
