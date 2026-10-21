package com.skillexchange.dto.credit;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditAccountDto {
    private BigDecimal balance;
    private BigDecimal totalEarned;
    private BigDecimal totalSpent;
}
