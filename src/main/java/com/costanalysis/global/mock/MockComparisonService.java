package com.costanalysis.global.mock;

import com.costanalysis.domain.comparison.dto.ComparisonCreateRequest;
import com.costanalysis.domain.comparison.dto.ComparisonResponse;
import com.costanalysis.domain.comparison.service.ComparisonService;
import com.costanalysis.domain.user.entity.User;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Mock 프로파일 전용 비교 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockComparisonService extends ComparisonService {

    private final MockDataStore dataStore;
    private final ConcurrentHashMap<Long, ComparisonResponse> comparisons = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(0);

    public MockComparisonService(MockDataStore dataStore) {
        super(null, null, null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public ComparisonResponse create(Long userId, ComparisonCreateRequest req) {
        User user = dataStore.getUsers().get(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        Long id = seq.incrementAndGet();
        String idsJson = "[" + req.getQuotationIds().stream()
                .map(String::valueOf).collect(Collectors.joining(",")) + "]";

        ComparisonResponse cr = ComparisonResponse.builder()
                .id(id)
                .title(req.getTitle())
                .quotationIds(idsJson)
                .status("DONE")
                .resultJson("{\"quotations\":[]}")
                .createdByName(user.getName())
                .createdAt(OffsetDateTime.now())
                .analyzedAt(OffsetDateTime.now())
                .build();
        comparisons.put(id, cr);
        log.debug("[MOCK] 비교 세션 생성: id={}", id);
        return cr;
    }

    @Override
    public Page<ComparisonResponse> listMine(Long userId, Pageable pageable) {
        User user = dataStore.getUsers().get(userId);
        String name = user != null ? user.getName() : "김관리";
        List<ComparisonResponse> filtered = comparisons.values().stream()
                .filter(c -> name.equals(c.getCreatedByName()))
                .sorted(Comparator.comparing(ComparisonResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<ComparisonResponse> sub = start >= filtered.size() ? Collections.emptyList() : filtered.subList(start, end);
        return new PageImpl<>(sub, pageable, filtered.size());
    }

    @Override
    public ComparisonResponse getDetail(Long id) {
        ComparisonResponse cr = comparisons.get(id);
        if (cr == null) throw new BusinessException(ErrorCode.COMPARISON_NOT_FOUND);
        return cr;
    }

    @Override
    public void delete(Long id) {
        if (!comparisons.containsKey(id)) throw new BusinessException(ErrorCode.COMPARISON_NOT_FOUND);
        comparisons.remove(id);
        log.debug("[MOCK] 비교 세션 삭제: id={}", id);
    }
}
