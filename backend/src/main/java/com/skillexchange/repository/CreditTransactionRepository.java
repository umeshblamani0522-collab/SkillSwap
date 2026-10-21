package com.skillexchange.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skillexchange.entity.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {
    Page<CreditTransaction> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    Optional<CreditTransaction> findByIdempotencyKey(String key);

    void deleteByAccountId(Long accountId);

    @Modifying
    @Query("""
                update CreditTransaction ct
                set ct.relatedSession = null
                where ct.relatedSession.id in :sessionIds
            """)
    int detachFromSessions(
            @Param("sessionIds") List<Long> sessionIds);
}
