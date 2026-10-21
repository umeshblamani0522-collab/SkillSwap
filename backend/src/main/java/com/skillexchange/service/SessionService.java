package com.skillexchange.service;

import com.skillexchange.dto.session.CreateSessionRequest;
import com.skillexchange.dto.session.SessionDto;
import com.skillexchange.entity.ExchangeRequest;
import com.skillexchange.entity.Session;
import com.skillexchange.entity.User;
import com.skillexchange.entity.enums.ExchangeStatus;
import com.skillexchange.entity.enums.NotificationType;
import com.skillexchange.entity.enums.SessionStatus;
import com.skillexchange.exception.AccessDeniedCustomException;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.ExchangeRequestRepository;
import com.skillexchange.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ExchangeRequestRepository exchangeRequestRepository;
    private final CreditService creditService;
    private final NotificationService notificationService;
    private final EntityMapper mapper;

    @Transactional
    public SessionDto schedule(Long userId, CreateSessionRequest request) {
        ExchangeRequest exchangeRequest = exchangeRequestRepository.findById(request.getExchangeRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found"));

        if (!exchangeRequest.getSender().getId().equals(userId) && !exchangeRequest.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedCustomException("You are not a participant in this exchange");
        }
        if (exchangeRequest.getStatus() != ExchangeStatus.ACCEPTED) {
            throw new BusinessRuleException("Sessions can only be scheduled for accepted exchange requests");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessRuleException("Session start time must be before end time");
        }
        if (request.getSessionDate().isEqual(LocalDate.now()) && !request.getEndTime().isAfter(LocalTime.now())) {
            throw new BusinessRuleException("A new session must end in the future");
        }
        long durationMinutes = java.time.Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        if (durationMinutes < 30 || durationMinutes > 8 * 60) {
            throw new BusinessRuleException("Session duration must be between 30 minutes and 8 hours");
        }

        // sender teaches offeredSkill to receiver in the first session of an exchange
        User teacher = exchangeRequest.getSender();
        User learner = exchangeRequest.getReceiver();

        // Business rule: no overlapping sessions for either participant
        List<Session> teacherOverlap = sessionRepository.findOverlapping(teacher.getId(), request.getSessionDate(),
                request.getStartTime(), request.getEndTime());
        List<Session> learnerOverlap = sessionRepository.findOverlapping(learner.getId(), request.getSessionDate(),
                request.getStartTime(), request.getEndTime());
        if (!teacherOverlap.isEmpty() || !learnerOverlap.isEmpty()) {
            throw new BusinessRuleException("This time slot overlaps with an existing session for one of the participants");
        }

        Session session = Session.builder()
                .exchangeRequest(exchangeRequest)
                .teacher(teacher).learner(learner)
                .skill(exchangeRequest.getOfferedSkill())
                .sessionDate(request.getSessionDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(SessionStatus.SCHEDULED)
                .meetingInfo(request.getMeetingInfo())
                .notes(request.getNotes())
                .build();
        session = sessionRepository.save(session);

        notificationService.notify(learner.getId(), NotificationType.SESSION_SCHEDULED,
                "A session for " + session.getSkill().getName() + " was scheduled", "/sessions/" + session.getId());
        notificationService.notify(teacher.getId(), NotificationType.SESSION_SCHEDULED,
                "A session for " + session.getSkill().getName() + " was scheduled", "/sessions/" + session.getId());

        return mapper.toDto(session);
    }

    @Transactional
    public SessionDto complete(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getTeacher().getId().equals(userId) && !session.getLearner().getId().equals(userId)) {
            throw new AccessDeniedCustomException("You are not a participant in this session");
        }
        // Business rule: a session cannot be completed twice
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessRuleException("This session has already been completed");
        }
        if (java.time.LocalDateTime.now().isBefore(java.time.LocalDateTime.of(session.getSessionDate(), session.getEndTime()))) {
            throw new BusinessRuleException("A session can only be completed after its scheduled end time");
        }
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessRuleException("A cancelled session cannot be completed");
        }

        if (session.getStatus() == SessionStatus.NO_SHOW) {
            throw new BusinessRuleException("A no-show session cannot be completed");
        }
        session.setStatus(SessionStatus.COMPLETED);
        sessionRepository.save(session);

        long durationHours = java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
        BigDecimal creditAmount = BigDecimal.valueOf(durationHours).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        if (creditAmount.compareTo(BigDecimal.ZERO) <= 0) creditAmount = BigDecimal.ONE;

        // Credits only transfer after successful completion, atomically, exactly once
        creditService.transferForCompletedSession(session.getTeacher(), session.getLearner(), creditAmount, session);
        session.setCreditsTransferred(true);
        sessionRepository.save(session);

        long activeSessions = sessionRepository.countByExchangeRequestIdAndStatus(session.getExchangeRequest().getId(), SessionStatus.SCHEDULED)
                + sessionRepository.countByExchangeRequestIdAndStatus(session.getExchangeRequest().getId(), SessionStatus.IN_PROGRESS);
        if (activeSessions == 0) {
            session.getExchangeRequest().setStatus(ExchangeStatus.COMPLETED);
        }

        notificationService.notify(session.getTeacher().getId(), NotificationType.CREDITS_TRANSFERRED,
                "You earned " + creditAmount + " credits for teaching " + session.getSkill().getName(), "/wallet");
        notificationService.notify(session.getLearner().getId(), NotificationType.SESSION_COMPLETED,
                "Your session for " + session.getSkill().getName() + " is complete. Please rate your experience!",
                "/sessions/" + session.getId());

        return mapper.toDto(session);
    }

    @Transactional
    public SessionDto cancel(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getTeacher().getId().equals(userId) && !session.getLearner().getId().equals(userId)) {
            throw new AccessDeniedCustomException("You are not a participant in this session");
        }
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessRuleException("A completed session cannot be cancelled");
        }
        if (session.getStatus() == SessionStatus.NO_SHOW) {
            throw new BusinessRuleException("A no-show session cannot be cancelled");
        }
        session.setStatus(SessionStatus.CANCELLED);
        return mapper.toDto(sessionRepository.save(session));
    }

    @Transactional
    public SessionDto markNoShow(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getTeacher().getId().equals(userId) && !session.getLearner().getId().equals(userId)) {
            throw new AccessDeniedCustomException("You are not a participant in this session");
        }
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.CANCELLED) {
            throw new BusinessRuleException("This session cannot be marked as no-show");
        }
        if (java.time.LocalDateTime.now().isBefore(java.time.LocalDateTime.of(session.getSessionDate(), session.getEndTime()))) {
            throw new BusinessRuleException("A session can only be marked no-show after its scheduled end time");
        }
        session.setStatus(SessionStatus.NO_SHOW);
        return mapper.toDto(sessionRepository.save(session));
    }

    @Transactional
    public void delete(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getTeacher().getId().equals(userId) && !session.getLearner().getId().equals(userId)) {
            throw new AccessDeniedCustomException("You are not a participant in this session");
        }
        sessionRepository.delete(session);
    }

    @Transactional(readOnly = true)
    public List<SessionDto> getMySessions(Long userId) {
        return sessionRepository.findByTeacherIdOrLearnerIdOrderByCreatedAtDesc(userId, userId)
                .stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SessionDto getById(Long userId, Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!session.getTeacher().getId().equals(userId) && !session.getLearner().getId().equals(userId)) {
            throw new AccessDeniedCustomException("You are not a participant in this session");
        }
        return mapper.toDto(session);
    }
}
