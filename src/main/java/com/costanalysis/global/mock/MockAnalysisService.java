package com.costanalysis.global.mock;

import com.costanalysis.domain.analysis.dto.AnalysisResult;
import com.costanalysis.domain.analysis.dto.AnalysisResult.AnomalyItem;
import com.costanalysis.domain.analysis.dto.AnalysisResult.CategoryGroup;
import com.costanalysis.domain.analysis.service.AnalysisService;
import com.costanalysis.global.exception.BusinessException;
import com.costanalysis.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock 프로파일 전용 분석 서비스.
 */
@Slf4j
@Service
@Profile("mock")
public class MockAnalysisService extends AnalysisService {

    private final MockDataStore dataStore;

    public MockAnalysisService(MockDataStore dataStore) {
        super(null, null); // 부모 의존성은 사용하지 않음
        this.dataStore = dataStore;
    }

    @Override
    public AnalysisResult analyze(Long quotationId) {
        Map<String, Object> q = dataStore.getQuotations().get(quotationId);
        if (q == null) throw new BusinessException(ErrorCode.QUOTATION_NOT_FOUND);

        Integer parsedItems = (Integer) q.get("parsedItems");
        Integer anomalies = (Integer) q.get("anomalies");
        int totalItems = parsedItems != null ? parsedItems : 10;
        int anomalyCount = anomalies != null ? anomalies : 0;

        List<CategoryGroup> groups = List.of(
                CategoryGroup.builder().category("재료비").itemCount(totalItems / 2).totalAmount(BigDecimal.valueOf(5_000_000)).ratio(BigDecimal.valueOf(0.50)).build(),
                CategoryGroup.builder().category("가공비").itemCount(totalItems / 3).totalAmount(BigDecimal.valueOf(3_000_000)).ratio(BigDecimal.valueOf(0.30)).build(),
                CategoryGroup.builder().category("제경비").itemCount(totalItems - totalItems / 2 - totalItems / 3).totalAmount(BigDecimal.valueOf(2_000_000)).ratio(BigDecimal.valueOf(0.20)).build()
        );

        List<AnomalyItem> anomalyItems = new ArrayList<>();
        for (int i = 0; i < anomalyCount; i++) {
            anomalyItems.add(AnomalyItem.builder()
                    .itemId((long) (i + 1))
                    .itemName("이상항목 " + (i + 1))
                    .unitPrice(BigDecimal.valueOf(150_000))
                    .avgUnitPrice(BigDecimal.valueOf(80_000))
                    .deviation(BigDecimal.valueOf(0.875))
                    .reason("카테고리 평균 단가 대비 87.5% 편차")
                    .build());
        }

        log.debug("[MOCK] 분석 완료: quotationId={}, items={}, anomalies={}", quotationId, totalItems, anomalyCount);
        return AnalysisResult.builder()
                .quotationId(quotationId)
                .totalItems(totalItems)
                .totalAmount(BigDecimal.valueOf(10_000_000))
                .categoryGroups(groups)
                .anomalies(anomalyItems)
                .build();
    }
}
