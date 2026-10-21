package com.skillexchange.service;

import com.skillexchange.dto.report.CreateReportRequest;
import com.skillexchange.dto.report.ReportDto;
import com.skillexchange.dto.report.ResolveReportRequest;
import com.skillexchange.entity.Report;
import com.skillexchange.entity.User;
import com.skillexchange.entity.enums.NotificationType;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.ReportRepository;
import com.skillexchange.repository.UserRepository;
import com.skillexchange.repository.MessageRepository;
import com.skillexchange.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SessionRepository sessionRepository;
    private final NotificationService notificationService;
    private final EntityMapper mapper;

    @Transactional
    public ReportDto create(Long reporterId, CreateReportRequest request) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter not found"));
        validateTarget(reporterId, request);
        Report report = Report.builder()
                .reporter(reporter)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .build();
        return mapper.toDto(reportRepository.save(report));
    }

    private void validateTarget(Long reporterId, CreateReportRequest request) {
        if (request.getTargetId() == null || request.getTargetId() <= 0) {
            throw new com.skillexchange.exception.BusinessRuleException("Report target must be a valid positive ID");
        }
        switch (request.getTargetType()) {
            case USER -> {
                User target = userRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Reported user not found"));
                if (target.getId().equals(reporterId)) throw new com.skillexchange.exception.BusinessRuleException("You cannot report your own account");
            }
            case MESSAGE -> {
                var message = messageRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Reported message not found"));
                if (!message.getSender().getId().equals(reporterId) && !message.getReceiver().getId().equals(reporterId))
                    throw new com.skillexchange.exception.AccessDeniedCustomException("You can only report a message from your conversation");
            }
            case SESSION -> {
                var session = sessionRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new ResourceNotFoundException("Reported session not found"));
                if (!session.getTeacher().getId().equals(reporterId) && !session.getLearner().getId().equals(reporterId))
                    throw new com.skillexchange.exception.AccessDeniedCustomException("You can only report a session you participated in");
            }
            case CONTENT -> { /* Content moderation target can reference a future moderation-managed resource. */ }
        }
    }

    public Page<ReportDto> getByStatus(com.skillexchange.entity.enums.ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatus(status, pageable).map(mapper::toDto);
    }

    @Transactional
    public ReportDto resolve(Long reportId, ResolveReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setStatus(request.getStatus());
        report.setResolutionNotes(request.getResolutionNotes());
        report = reportRepository.save(report);

        notificationService.notify(report.getReporter().getId(), NotificationType.REPORT_STATUS_CHANGED,
                "Your report status changed to " + request.getStatus().name(), "/reports/" + report.getId());

        return mapper.toDto(report);
    }
}
