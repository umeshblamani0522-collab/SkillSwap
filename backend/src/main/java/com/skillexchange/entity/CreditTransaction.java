package com.skillexchange.entity;

import com.skillexchange.entity.enums.CreditTransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_transactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = "idempotency_key")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private CreditAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditTransactionType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_session_id")
    private Session relatedSession;

    @Column(length = 300)
    private String description;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    @Builder.Default
    private String idempotencyKey = UUID.randomUUID().toString();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
