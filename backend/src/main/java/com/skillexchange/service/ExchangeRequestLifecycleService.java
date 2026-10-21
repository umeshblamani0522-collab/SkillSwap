package com.skillexchange.service;

import com.skillexchange.entity.ExchangeRequest;
import com.skillexchange.entity.enums.ExchangeStatus;
import com.skillexchange.repository.ExchangeRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExchangeRequestLifecycleService {
    private final ExchangeRequestRepository exchangeRequestRepository;

    @Scheduled(fixedDelayString = "${app.exchange-request.expiry-check-ms:300000}", initialDelayString = "${app.exchange-request.expiry-initial-delay-ms:30000}")
    @Transactional
    public void expirePendingRequests() {
        LocalDateTime now = LocalDateTime.now();
        for (ExchangeRequest request : exchangeRequestRepository.findByStatus(ExchangeStatus.PENDING)) {
            if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(now)) {
                request.setStatus(ExchangeStatus.EXPIRED);
            }
        }
    }
}
