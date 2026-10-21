
package com.skillexchange.repository;

import com.skillexchange.entity.ExchangeRequest;
import com.skillexchange.entity.enums.ExchangeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeRequestRepository
                extends JpaRepository<ExchangeRequest, Long> {

        Page<ExchangeRequest> findByReceiverIdAndStatus(
                        Long receiverId,
                        ExchangeStatus status,
                        Pageable pageable);

        Page<ExchangeRequest> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(
                        Long senderId,
                        Long receiverId,
                        Pageable pageable);

        // Find all exchange requests involving a user.
        List<ExchangeRequest> findBySenderIdOrReceiverId(
                        Long senderId,
                        Long receiverId);

        // Delete exchange requests involving a user.
        void deleteBySenderIdOrReceiverId(
                        Long senderId,
                        Long receiverId);

        List<ExchangeRequest> findByStatus(ExchangeStatus status);

        long countByStatus(ExchangeStatus status);

        boolean existsBySenderIdAndReceiverIdAndStatus(
                        Long senderId,
                        Long receiverId,
                        ExchangeStatus status);
}
