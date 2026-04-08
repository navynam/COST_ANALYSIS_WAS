package com.costanalysis.global.mock;

import com.costanalysis.domain.user.entity.User;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mock 프로파일 전용 인메모리 데이터 저장소.
 * 프론트엔드 mockData와 동일한 데이터를 보유한다.
 */
@Slf4j
@Component
@Profile("mock")
@Getter
public class MockDataStore {

    // ── 시퀀스 ──
    private final AtomicLong userSeq         = new AtomicLong(10);
    private final AtomicLong quotationSeq    = new AtomicLong(30);
    private final AtomicLong formulaSeq      = new AtomicLong(10);
    private final AtomicLong changeReqSeq    = new AtomicLong(10);
    private final AtomicLong notificationSeq = new AtomicLong(10);
    private final AtomicLong noteSeq         = new AtomicLong(10);

    // ── 데이터 저장소 ──
    private final ConcurrentHashMap<Long, User> users = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Map<String, Object>> quotations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Map<String, Object>> formulas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Map<String, Object>> changeRequests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Map<String, Object>> notifications = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Map<String, Object>> notes = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("=== MockDataStore 초기화 시작 ===");
        initUsers();
        initQuotations();
        initFormulas();
        initChangeRequests();
        initNotifications();
        log.info("=== MockDataStore 초기화 완료: 사용자 {}명, 견적서 {}건, 수식 {}개, 변경요청 {}건, 알림 {}건 ===",
                users.size(), quotations.size(), formulas.size(), changeRequests.size(), notifications.size());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 사용자 3명
    // ══════════════════════════════════════════════════════════════════════════
    private void initUsers() {
        users.put(1L, User.builder()
                .id(1L).employeeId("admin1").password("$2a$10$dummyhash").name("김관리")
                .department("시스템관리팀").phone("010-1111-0001").role("ADMIN")
                .language("ko").notifyEmail(true).notifyInApp(true).active(true)
                .createdAt(OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.of("+09:00")))
                .build());

        users.put(2L, User.builder()
                .id(2L).employeeId("analyst1").password("$2a$10$dummyhash").name("이분석")
                .department("견적1팀").phone("010-2222-0002").role("USER")
                .language("ko").notifyEmail(true).notifyInApp(true).active(true)
                .createdAt(OffsetDateTime.of(2025, 3, 15, 0, 0, 0, 0, ZoneOffset.of("+09:00")))
                .build());

        users.put(3L, User.builder()
                .id(3L).employeeId("verifier1").password("$2a$10$dummyhash").name("박검증")
                .department("견적2팀").phone("010-3333-0003").role("USER")
                .language("ko").notifyEmail(true).notifyInApp(true).active(true)
                .createdAt(OffsetDateTime.of(2025, 6, 1, 0, 0, 0, 0, ZoneOffset.of("+09:00")))
                .build());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 견적서 20건 (프론트엔드 initialFiles와 동일)
    // ══════════════════════════════════════════════════════════════════════════
    private void initQuotations() {
        addQuotation(1L,  "CONSOLE_BOX_원가명세.xlsx",       "extracting", 65,  null, null, "2026-02-19", "2.4 MB", 3, "김영수", "구매팀");
        addQuotation(2L,  "HEAD_LINING_원가계산서.xlsx",      "verifying",  100, 24,   2,    "2026-02-18", "3.1 MB", 4, "박지혜", "원가관리팀");
        addQuotation(3L,  "DOOR_TRIM_견적서.xlsx",            "verified",   100, 18,   0,    "2026-02-17", "1.8 MB", 2, "이철수", "구매팀");
        addQuotation(4L,  "SEAT_COVER_원가분석.xlsx",         "failed",     30,  null, null, "2026-02-16", "4.2 MB", 1, "홍길동", "품질팀");
        addQuotation(5L,  "BUMPER_ASSY_Q4견적.xlsx",          "analyzed",   100, 32,   0,    "2026-02-15", "2.9 MB", 3, "최민정", "원가관리팀");
        addQuotation(6L,  "FENDER_PANEL_원가산출.xlsx",       "extracting", 42,  null, null, "2026-02-14", "1.9 MB", 2, "정하늘", "견적1팀");
        addQuotation(7L,  "RADIATOR_GRILLE_견적서.xlsx",      "verifying",  100, 28,   1,    "2026-02-13", "2.7 MB", 3, "김민수", "견적2팀");
        addQuotation(8L,  "HOOD_INNER_원가계산서.xlsx",       "verified",   100, 22,   3,    "2026-02-12", "3.5 MB", 4, "이분석", "견적1팀");
        addQuotation(9L,  "WHEEL_COVER_견적서.xlsx",          "analyzing",  100, 15,   1,    "2026-02-11", "1.5 MB", 2, "박검증", "견적2팀");
        addQuotation(10L, "TRUNK_LID_원가분석.xlsx",          "analyzed",   100, 26,   2,    "2026-02-10", "2.8 MB", 3, "김영수", "구매팀");
        addQuotation(11L, "SIDE_MIRROR_원가명세.xlsx",        "extracting", 88,  null, null, "2026-02-09", "2.1 MB", 2, "최민정", "원가관리팀");
        addQuotation(12L, "AIR_BAG_MODULE_견적.xlsx",         "verifying",  100, 35,   4,    "2026-02-08", "4.8 MB", 5, "정하늘", "견적1팀");
        addQuotation(13L, "CLUSTER_원가계산서.xlsx",           "verified",   100, 20,   0,    "2026-02-07", "2.2 MB", 3, "홍길동", "품질팀");
        addQuotation(14L, "STEERING_WHEEL_견적.xlsx",         "failed",     15,  null, null, "2026-02-06", "5.1 MB", 1, "박지혜", "원가관리팀");
        addQuotation(15L, "WIPER_ARM_원가분석.xlsx",          "analyzed",   100, 12,   0,    "2026-02-05", "1.3 MB", 2, "이철수", "구매팀");
        addQuotation(16L, "ROOF_RACK_견적서.xlsx",            "analyzing",  100, 19,   2,    "2026-02-04", "2.6 MB", 3, "김민수", "견적2팀");
        addQuotation(17L, "TAIL_LAMP_원가계산서.xlsx",        "extracting", 23,  null, null, "2026-02-03", "3.3 MB", 4, "이분석", "견적1팀");
        addQuotation(18L, "EXHAUST_PIPE_견적서.xlsx",         "verified",   100, 14,   1,    "2026-02-02", "1.7 MB", 2, "박검증", "견적2팀");
        addQuotation(19L, "BRAKE_PAD_원가분석.xlsx",          "failed",     50,  null, null, "2026-02-01", "3.9 MB", 1, "김영수", "구매팀");
        addQuotation(20L, "SUSPENSION_ARM_견적.xlsx",         "analyzed",   100, 29,   1,    "2026-01-31", "2.5 MB", 3, "최민정", "원가관리팀");
    }

    private void addQuotation(Long id, String name, String status, int progress,
                              Integer parsedItems, Integer anomalies,
                              String uploadDate, String fileSize, int sheets,
                              String uploader, String department) {
        Map<String, Object> q = new ConcurrentHashMap<>();
        q.put("id", id);
        q.put("name", name);
        q.put("status", status);
        q.put("progress", progress);
        if (parsedItems != null) q.put("parsedItems", parsedItems);
        if (anomalies != null) q.put("anomalies", anomalies);
        q.put("uploadDate", uploadDate);
        q.put("fileSize", fileSize);
        q.put("sheets", sheets);
        q.put("uploader", uploader);
        q.put("department", department);
        q.put("fileType", "XLSX");
        q.put("createdAt", OffsetDateTime.of(LocalDate.parse(uploadDate).atStartOfDay(), ZoneOffset.of("+09:00")));
        quotations.put(id, q);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 수식 4개 (프론트엔드 initialFormulas와 동일)
    // ══════════════════════════════════════════════════════════════════════════
    private void initFormulas() {
        OffsetDateTime now = OffsetDateTime.now();

        formulas.put(1L, makeFormula(1L, "생산원가", "core",
                "생산원가 = 재료비 + 가공비 + 제경비",
                "제품의 총 생산원가를 산출하는 핵심 수식입니다.",
                true, true, List.of("전체"), now));

        formulas.put(2L, makeFormula(2L, "재료비 소계", "sub",
                "재료비 = Σ(단가 × 수량 × (1 + 로스율))",
                "원자재 및 부자재의 합계를 산출합니다. 로스율을 반영합니다.",
                true, true, List.of("전체"), now));

        formulas.put(3L, makeFormula(3L, "가공비 단가", "sub",
                "가공비 = (설비감가상각 + 인건비) / 생산수량 × CT",
                "공정별 가공비 단가를 산출합니다. CT는 사이클타임(분)입니다.",
                true, true, List.of("전체"), now));

        formulas.put(4L, makeFormula(4L, "제경비율", "rate",
                "제경비율 = 제경비 / (재료비 + 가공비) × 100",
                "제경비의 비율을 산출합니다. 일반적 범위: 8~15%",
                false, true, List.of("견적1팀", "견적2팀"), now));
    }

    private Map<String, Object> makeFormula(Long id, String name, String category,
                                            String formula, String description,
                                            boolean systemFormula, boolean active,
                                            List<String> departments, OffsetDateTime ts) {
        Map<String, Object> f = new ConcurrentHashMap<>();
        f.put("id", id);
        f.put("name", name);
        f.put("category", category);
        f.put("formula", formula);
        f.put("description", description);
        f.put("systemFormula", systemFormula);
        f.put("active", active);
        f.put("departments", new ArrayList<>(departments));
        f.put("createdAt", ts);
        f.put("updatedAt", ts);
        return f;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 변경요청 2건 (프론트엔드 initialRequests와 동일)
    // ══════════════════════════════════════════════════════════════════════════
    private void initChangeRequests() {
        Map<String, Object> cr1 = new ConcurrentHashMap<>();
        cr1.put("id", 1L);
        cr1.put("formulaId", 2L);
        cr1.put("requesterId", 2L);
        cr1.put("requesterName", "이분석");
        cr1.put("department", "견적1팀");
        cr1.put("taskName", "HEAD_LINING 원가분석");
        cr1.put("originalFormula", "{\"name\":\"재료비 소계\",\"expression\":\"재료비 = Σ(단가 × 수량 × (1 + 로스율))\"}");
        cr1.put("modifiedFields", "{\"expression\":\"재료비 = Σ(단가 × 수량 × (1 + 로스율) × 환율보정계수)\",\"variables\":[\"단가\",\"수량\",\"로스율\",\"환율보정계수\"]}");
        cr1.put("status", "PENDING");
        cr1.put("reason", "HEAD_LINING 수입 원자재에 환율 보정계수 반영이 필요합니다.");
        cr1.put("createdAt", OffsetDateTime.of(2026, 4, 5, 14, 30, 0, 0, ZoneOffset.of("+09:00")));
        // ConcurrentHashMap does not allow null values; omit null fields
        changeRequests.put(1L, cr1);

        Map<String, Object> cr2 = new ConcurrentHashMap<>();
        cr2.put("id", 2L);
        cr2.put("formulaId", 4L);
        cr2.put("requesterId", 3L);
        cr2.put("requesterName", "박검증");
        cr2.put("department", "견적2팀");
        cr2.put("taskName", "DOOR_TRIM 견적검증");
        cr2.put("originalFormula", "{\"name\":\"제경비율\",\"expression\":\"제경비율 = 제경비 / (재료비 + 가공비) × 100\"}");
        cr2.put("modifiedFields", "{\"expression\":\"제경비율 = (제경비 + 물류비) / (재료비 + 가공비) × 100\",\"variables\":[\"제경비\",\"물류비\",\"재료비\",\"가공비\"]}");
        cr2.put("status", "APPROVED");
        cr2.put("reason", "DOOR_TRIM 해외 납품건 물류비를 제경비에 포함해야 합니다.");
        cr2.put("createdAt", OffsetDateTime.of(2026, 4, 3, 9, 15, 0, 0, ZoneOffset.of("+09:00")));
        cr2.put("reviewedAt", OffsetDateTime.of(2026, 4, 4, 11, 0, 0, 0, ZoneOffset.of("+09:00")));
        cr2.put("reviewerComment", "물류비 포함 타당, 견적2팀에 한해 승인합니다.");
        cr2.put("approvedDepartments", List.of("견적2팀"));
        changeRequests.put(2L, cr2);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 알림 데이터
    // ══════════════════════════════════════════════════════════════════════════
    private void initNotifications() {
        OffsetDateTime now = OffsetDateTime.now();
        addNotification(1L, 1L, "PARSING_DONE", "견적서 파싱 완료", "HEAD_LINING_원가계산서.xlsx 파싱이 완료되었습니다.", 2L, false, now.minusHours(2));
        addNotification(2L, 1L, "VERIFICATION_REQUIRED", "검증 대기", "RADIATOR_GRILLE_견적서.xlsx 검증이 필요합니다.", 7L, false, now.minusHours(5));
        addNotification(3L, 2L, "SYSTEM", "변경 요청 등록", "수식 변경 요청이 정상 등록되었습니다.", 1L, true, now.minusDays(1));
        addNotification(4L, 3L, "PARSING_DONE", "견적서 파싱 완료", "WHEEL_COVER_견적서.xlsx 파싱이 완료되었습니다.", 9L, false, now.minusDays(2));
        addNotification(5L, 1L, "SYSTEM", "시스템 공지", "원가분석 시스템 v2.0이 배포되었습니다.", null, true, now.minusDays(7));
    }

    private void addNotification(Long id, Long userId, String type, String title, String body,
                                 Long refId, boolean readFlag, OffsetDateTime createdAt) {
        Map<String, Object> n = new ConcurrentHashMap<>();
        n.put("id", id);
        n.put("userId", userId);
        n.put("type", type);
        n.put("title", title);
        n.put("body", body);
        if (refId != null) n.put("refId", refId);
        n.put("readFlag", readFlag);
        n.put("createdAt", createdAt);
        notifications.put(id, n);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 유틸 메서드
    // ══════════════════════════════════════════════════════════════════════════

    public Long nextQuotationId() { return quotationSeq.incrementAndGet(); }
    public Long nextFormulaId()   { return formulaSeq.incrementAndGet(); }
    public Long nextChangeReqId() { return changeReqSeq.incrementAndGet(); }
    public Long nextNotificationId() { return notificationSeq.incrementAndGet(); }
    public Long nextNoteId()      { return noteSeq.incrementAndGet(); }

    public Optional<User> findUserByEmployeeId(String employeeId) {
        return users.values().stream()
                .filter(u -> u.getEmployeeId().equals(employeeId))
                .findFirst();
    }
}
