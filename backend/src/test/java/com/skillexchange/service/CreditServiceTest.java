package com.skillexchange.service;

import com.skillexchange.entity.CreditAccount;
import com.skillexchange.entity.Session;
import com.skillexchange.entity.User;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.CreditAccountRepository;
import com.skillexchange.repository.CreditTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceTest {

    @Mock private CreditAccountRepository creditAccountRepository;
    @Mock private CreditTransactionRepository creditTransactionRepository;
    @Mock private EntityMapper mapper;

    @InjectMocks
    private CreditService creditService;

    @Test
    void cannotSpendMoreCreditsThanBalance() {
        User teacher = User.builder().id(1L).username("teacher").build();
        User learner = User.builder().id(2L).username("learner").build();
        Session session = Session.builder().id(100L).teacher(teacher).learner(learner).build();

        CreditAccount teacherAccount = CreditAccount.builder().id(10L).user(teacher).balance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO).totalSpent(BigDecimal.ZERO).build();
        CreditAccount learnerAccount = CreditAccount.builder().id(20L).user(learner).balance(BigDecimal.valueOf(1))
                .totalEarned(BigDecimal.ZERO).totalSpent(BigDecimal.ZERO).build();

        when(creditTransactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(creditAccountRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(teacherAccount));
        when(creditAccountRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(learnerAccount));

        assertThrows(BusinessRuleException.class, () ->
                creditService.transferForCompletedSession(teacher, learner, BigDecimal.valueOf(5), session));
    }

    @Test
    void successfulTransferMovesCreditsAtomically() {
        User teacher = User.builder().id(1L).username("teacher").build();
        User learner = User.builder().id(2L).username("learner").build();
        Session session = Session.builder().id(100L).teacher(teacher).learner(learner).build();

        CreditAccount teacherAccount = CreditAccount.builder().id(10L).user(teacher).balance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO).totalSpent(BigDecimal.ZERO).build();
        CreditAccount learnerAccount = CreditAccount.builder().id(20L).user(learner).balance(BigDecimal.valueOf(5))
                .totalEarned(BigDecimal.ZERO).totalSpent(BigDecimal.ZERO).build();

        when(creditTransactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(creditAccountRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(teacherAccount));
        when(creditAccountRepository.findByUserIdForUpdate(2L)).thenReturn(Optional.of(learnerAccount));

        creditService.transferForCompletedSession(teacher, learner, BigDecimal.valueOf(2), session);

        assertEquals(BigDecimal.valueOf(2), teacherAccount.getBalance());
        assertEquals(BigDecimal.valueOf(3), learnerAccount.getBalance());
        verify(creditTransactionRepository, times(2)).save(any());
    }

    @Test
    void doubleCompletionDoesNotDoubleTransferCredits() {
        User teacher = User.builder().id(1L).username("teacher").build();
        User learner = User.builder().id(2L).username("learner").build();
        Session session = Session.builder().id(100L).teacher(teacher).learner(learner).build();

        when(creditTransactionRepository.findByIdempotencyKey("session-complete-100-debit"))
                .thenReturn(Optional.of(new com.skillexchange.entity.CreditTransaction()));

        creditService.transferForCompletedSession(teacher, learner, BigDecimal.valueOf(2), session);

        verify(creditAccountRepository, never()).findByUserIdForUpdate(any());
        verify(creditTransactionRepository, never()).save(any());
    }
}
