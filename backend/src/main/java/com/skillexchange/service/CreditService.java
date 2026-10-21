package com.skillexchange.service;

import com.skillexchange.dto.credit.CreditAccountDto;
import com.skillexchange.dto.credit.CreditTransactionDto;
import com.skillexchange.entity.CreditAccount;
import com.skillexchange.entity.CreditTransaction;
import com.skillexchange.entity.Session;
import com.skillexchange.entity.User;
import com.skillexchange.entity.enums.CreditTransactionType;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.CreditAccountRepository;
import com.skillexchange.repository.CreditTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * All credit-balance mutations go through this service so that debits/credits
 * are atomic, idempotent, and never allow a negative balance.
 */
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditAccountRepository creditAccountRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public CreditAccountDto getAccount(Long userId) {
        CreditAccount account = creditAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit account not found"));
        return mapper.toDto(account);
    }

    @Transactional(readOnly = true)
    public Page<CreditTransactionDto> getHistory(Long userId, Pageable pageable) {
        CreditAccount account = creditAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit account not found"));
        return creditTransactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId(), pageable)
                .map(mapper::toDto);
    }

    /**
     * Atomically transfers `amount` credits from learner to teacher for a completed session.
     * Uses a row lock on both accounts (ordered by id to avoid deadlocks) and an
     * idempotency key so a retried request never double-transfers.
     */
    @Transactional
    public void transferForCompletedSession(User teacher, User learner, BigDecimal amount, Session session) {
        String idempotencyKey = "session-complete-" + session.getId();
        if (creditTransactionRepository.findByIdempotencyKey(idempotencyKey + "-debit").isPresent()) {
            return; // already processed - guarantees a session cannot be paid out twice
        }

        Long firstId = Math.min(teacher.getId(), learner.getId());
        Long secondId = Math.max(teacher.getId(), learner.getId());
        CreditAccount firstLocked = creditAccountRepository.findByUserIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit account not found"));
        CreditAccount secondLocked = creditAccountRepository.findByUserIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit account not found"));

        CreditAccount teacherAccount = teacher.getId().equals(firstId) ? firstLocked : secondLocked;
        CreditAccount learnerAccount = learner.getId().equals(firstId) ? firstLocked : secondLocked;

        if (learnerAccount.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Learner does not have enough skill credits to complete this session");
        }

        learnerAccount.setBalance(learnerAccount.getBalance().subtract(amount));
        learnerAccount.setTotalSpent(learnerAccount.getTotalSpent().add(amount));
        creditAccountRepository.save(learnerAccount);

        teacherAccount.setBalance(teacherAccount.getBalance().add(amount));
        teacherAccount.setTotalEarned(teacherAccount.getTotalEarned().add(amount));
        creditAccountRepository.save(teacherAccount);

        creditTransactionRepository.save(CreditTransaction.builder()
                .account(learnerAccount)
                .type(CreditTransactionType.SPENT_LEARNING)
                .amount(amount.negate())
                .balanceAfter(learnerAccount.getBalance())
                .relatedSession(session)
                .description("Spent on session #" + session.getId())
                .idempotencyKey(idempotencyKey + "-debit")
                .build());

        creditTransactionRepository.save(CreditTransaction.builder()
                .account(teacherAccount)
                .type(CreditTransactionType.EARNED_TEACHING)
                .amount(amount)
                .balanceAfter(teacherAccount.getBalance())
                .relatedSession(session)
                .description("Earned teaching session #" + session.getId())
                .idempotencyKey(idempotencyKey + "-credit")
                .build());
    }

    @Transactional
    public void adminAdjust(Long userId, BigDecimal amount, String reason) {
        CreditAccount account = creditAccountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit account not found"));
        BigDecimal newBalance = account.getBalance().add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Adjustment would result in a negative balance");
        }
        account.setBalance(newBalance);
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            account.setTotalEarned(account.getTotalEarned().add(amount));
        } else {
            account.setTotalSpent(account.getTotalSpent().add(amount.abs()));
        }
        creditAccountRepository.save(account);

        creditTransactionRepository.save(CreditTransaction.builder()
                .account(account)
                .type(CreditTransactionType.ADJUSTMENT_ADMIN)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .description(reason)
                .build());
    }
}
