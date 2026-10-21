package com.skillexchange.service;

import com.skillexchange.dto.admin.AdminUserDto;
import com.skillexchange.dto.admin.DashboardStatsDto;
import com.skillexchange.entity.AuditLog;
import com.skillexchange.entity.User;
import com.skillexchange.entity.enums.ExchangeStatus;
import com.skillexchange.entity.enums.ReportStatus;
import com.skillexchange.entity.enums.SessionStatus;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ExchangeRequestRepository exchangeRequestRepository;
    private final SessionRepository sessionRepository;
    private final RatingRepository ratingRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserSkillRepository userSkillRepository;

    public Page<AdminUserDto> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toAdminDto);
    }

    @Transactional
    public AdminUserDto setSuspended(Long adminId, Long userId, boolean suspended, String reason) {
        User admin = userRepository.findById(adminId).orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (adminId.equals(userId)) {
            throw new com.skillexchange.exception.BusinessRuleException("An administrator cannot suspend their own account");
        }
        user.setSuspended(suspended);
        userRepository.save(user);

        auditLogRepository.save(AuditLog.builder()
                .admin(admin)
                .action(suspended ? "SUSPEND_USER" : "UNSUSPEND_USER")
                .targetDescription("User #" + user.getId() + " (" + user.getUsername() + ")")
                .details(reason)
                .build());

        return toAdminDto(user);
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long newUsers = userRepository.countCreatedSince(LocalDateTime.now().minusDays(30));
        long totalExchanges = exchangeRequestRepository.count();
        long successfulExchanges = exchangeRequestRepository.countByStatus(ExchangeStatus.COMPLETED);
        long cancelledExchanges = exchangeRequestRepository.countByStatus(ExchangeStatus.CANCELLED);
        long totalSessions = sessionRepository.count();
        long completedSessions = sessionRepository.countByStatus(SessionStatus.COMPLETED);
        double avgRating = ratingRepository.findAll().stream()
                .filter(r -> r.getStars() != null)
                .mapToInt(r -> r.getStars()).average().orElse(0.0);
        long openReports = reportRepository.countByStatus(ReportStatus.OPEN);

        List<Map<String, Object>> popularSkills = userSkillRepository.findAll().stream()
                .filter(us -> us.getSkill() != null && us.getSkill().getName() != null)
                .collect(java.util.stream.Collectors.groupingBy(us -> us.getSkill().getName(), java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> Map.<String, Object>of("skill", e.getKey(), "count", e.getValue()))
                .toList();

        List<Map<String, Object>> mostActiveUsers = sessionRepository.findAll().stream()
                .flatMap(s -> {
                    String t = (s.getTeacher() != null) ? s.getTeacher().getUsername() : null;
                    String l = (s.getLearner() != null) ? s.getLearner().getUsername() : null;
                    return java.util.stream.Stream.of(t, l);
                })
                .filter(u -> u != null)
                .collect(java.util.stream.Collectors.groupingBy(u -> u, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> Map.<String, Object>of("username", e.getKey(), "sessions", e.getValue()))
                .toList();

        return DashboardStatsDto.builder()
                .totalUsers(totalUsers).activeUsers(activeUsers).newUsersLast30Days(newUsers)
                .totalExchanges(totalExchanges).successfulExchanges(successfulExchanges)
                .cancelledExchanges(cancelledExchanges)
                .totalSessions(totalSessions).completedSessions(completedSessions)
                .averageRating(Math.round(avgRating * 100.0) / 100.0)
                .openReports(openReports)
                .popularSkills(popularSkills)
                .mostActiveUsers(mostActiveUsers)
                .build();
    }

    private AdminUserDto toAdminDto(User u) {
        return AdminUserDto.builder()
                .id(u.getId()).username(u.getUsername()).email(u.getEmail()).fullName(u.getFullName())
                .suspended(u.isSuspended()).enabled(u.isEnabled())
                .roles(u.getRoles().stream().map(r -> r.getName().name()).toList())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
