package com.skillexchange.service;

import com.skillexchange.entity.*;
import com.skillexchange.entity.enums.ExchangeStatus;
import com.skillexchange.entity.enums.SessionStatus;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.ExchangeRequestRepository;
import com.skillexchange.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private ExchangeRequestRepository exchangeRequestRepository;
    @Mock private CreditService creditService;
    @Mock private NotificationService notificationService;
    @Mock private EntityMapper mapper;

    @InjectMocks
    private SessionService sessionService;

    private User teacher = User.builder().id(1L).username("teacher").build();
    private User learner = User.builder().id(2L).username("learner").build();
    private Skill skill = Skill.builder().id(5L).name("Java").build();

    @Test
    void cannotCompleteSessionTwice() {
        Session session = Session.builder().id(1L).teacher(teacher).learner(learner).skill(skill)
                .status(SessionStatus.COMPLETED).sessionDate(LocalDate.now().minusDays(1))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(BusinessRuleException.class, () -> sessionService.complete(1L, 1L));
        verify(creditService, never()).transferForCompletedSession(any(), any(), any(), any());
    }

    @Test
    void completingSessionTransfersCreditsExactlyOnce() {
        Session session = Session.builder().id(1L).teacher(teacher).learner(learner).skill(skill)
                .status(SessionStatus.SCHEDULED).sessionDate(LocalDate.now().minusDays(1))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0)).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.countByExchangeRequestIdAndStatus(anyLong(), eq(SessionStatus.SCHEDULED))).thenReturn(0L);
        when(sessionRepository.countByExchangeRequestIdAndStatus(anyLong(), eq(SessionStatus.IN_PROGRESS))).thenReturn(0L);

        sessionService.complete(1L, 1L);

        assertEquals(SessionStatus.COMPLETED, session.getStatus());
        verify(creditService, times(1)).transferForCompletedSession(eq(teacher), eq(learner), any(), eq(session));
    }

    @Test
    void schedulingRequiresAcceptedExchange() {
        ExchangeRequest er = ExchangeRequest.builder().id(1L).sender(teacher).receiver(learner)
                .offeredSkill(skill).requestedSkill(skill).status(ExchangeStatus.PENDING).build();
        when(exchangeRequestRepository.findById(1L)).thenReturn(Optional.of(er));

        var request = com.skillexchange.dto.session.CreateSessionRequest.builder()
                .exchangeRequestId(1L)
                .sessionDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .build();

        assertThrows(BusinessRuleException.class, () -> sessionService.schedule(1L, request));
    }

    @Test
    void startTimeMustBeBeforeEndTime() {
        ExchangeRequest er = ExchangeRequest.builder().id(1L).sender(teacher).receiver(learner)
                .offeredSkill(skill).requestedSkill(skill).status(ExchangeStatus.ACCEPTED).build();
        when(exchangeRequestRepository.findById(1L)).thenReturn(Optional.of(er));

        var request = com.skillexchange.dto.session.CreateSessionRequest.builder()
                .exchangeRequestId(1L)
                .sessionDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(12, 0)).endTime(LocalTime.of(11, 0))
                .build();

        assertThrows(BusinessRuleException.class, () -> sessionService.schedule(1L, request));
    }
}
