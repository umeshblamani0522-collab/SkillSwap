package com.skillexchange.dto.admin;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long newUsersLast30Days;
    private long totalExchanges;
    private long successfulExchanges;
    private long cancelledExchanges;
    private long totalSessions;
    private long completedSessions;
    private double averageRating;
    private long openReports;
    private List<Map<String, Object>> popularSkills;
    private List<Map<String, Object>> mostActiveUsers;
}
