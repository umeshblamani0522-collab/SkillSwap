package com.skillexchange.service;

import com.skillexchange.dto.exchange.CreateExchangeRequestDto;
import com.skillexchange.dto.match.MatchResultDto;
import com.skillexchange.entity.Skill;
import com.skillexchange.entity.User;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.ExchangeRequestRepository;
import com.skillexchange.repository.SkillRepository;
import com.skillexchange.repository.UserRepository;
import com.skillexchange.repository.UserSkillRepository;
import com.skillexchange.repository.ChatConnectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRequestServiceTest {

    @Mock private ExchangeRequestRepository exchangeRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private MatchingService matchingService;
    @Mock private NotificationService notificationService;
    @Mock private EntityMapper mapper;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private ChatConnectionRepository chatConnectionRepository;

    @InjectMocks
    private ExchangeRequestService exchangeRequestService;

    @Test
    void cannotSendRequestToSelf() {
        CreateExchangeRequestDto dto = CreateExchangeRequestDto.builder()
                .receiverId(1L).offeredSkillId(1L).requestedSkillId(2L).durationHours(1.0).build();

        assertThrows(BusinessRuleException.class, () -> exchangeRequestService.create(1L, dto));
        verifyNoInteractions(exchangeRequestRepository);
    }

    @Test
    void suspendedSenderCannotCreateExchange() {
        User sender = User.builder().id(1L).username("sender").suspended(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));

        CreateExchangeRequestDto dto = CreateExchangeRequestDto.builder()
                .receiverId(2L).offeredSkillId(1L).requestedSkillId(2L).durationHours(1.0).build();

        assertThrows(BusinessRuleException.class, () -> exchangeRequestService.create(1L, dto));
    }

    @Test
    void validRequestIsPersistedWithComputedMatchPercentage() {
        User sender = User.builder().id(1L).username("sender").suspended(false).build();
        User receiver = User.builder().id(2L).username("receiver").suspended(false).build();
        Skill offered = Skill.builder().id(10L).name("Java").active(true).build();
        Skill requested = Skill.builder().id(20L).name("UI/UX").active(true).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(skillRepository.findById(10L)).thenReturn(Optional.of(offered));
        when(skillRepository.findById(20L)).thenReturn(Optional.of(requested));
        when(userSkillRepository.existsByUserIdAndSkillIdAndSkillType(anyLong(), anyLong(), any())).thenReturn(true);
        when(exchangeRequestRepository.existsBySenderIdAndReceiverIdAndStatus(anyLong(), anyLong(), any())).thenReturn(false);
        when(chatConnectionRepository.findConnectionBetween(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(matchingService.calculateMatchBetween(1L, 2L))
                .thenReturn(MatchResultDto.builder().overallMatchPercentage(77.5).build());
        when(exchangeRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateExchangeRequestDto dto = CreateExchangeRequestDto.builder()
                .receiverId(2L).offeredSkillId(10L).requestedSkillId(20L).durationHours(2.0)
                .message("Let's trade").build();

        exchangeRequestService.create(1L, dto);

        verify(exchangeRequestRepository).save(argThat(er -> er.getMatchPercentage() == 77.5));
        verify(notificationService).notify(eq(2L), any(), any(), any());
    }
}
