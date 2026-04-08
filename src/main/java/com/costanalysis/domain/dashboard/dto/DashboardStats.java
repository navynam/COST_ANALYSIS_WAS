package com.costanalysis.domain.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Builder
public class DashboardStats {
    private long   totalQuotations;
    private long   parsedToday;
    private long   pendingVerification;
    private long   totalUsers;

    private BigDecimal totalAmountThisMonth;

    private List<RecentQuotation> recentQuotations;
    private List<StatusCount>     statusBreakdown;

    @Getter @Builder
    public static class RecentQuotation {
        private Long   id;
        private String filename;
        private String status;
        private String uploaderName;
        private String createdAt;
    }

    @Getter @Builder
    public static class StatusCount {
        private String status;
        private long   count;
    }
}
