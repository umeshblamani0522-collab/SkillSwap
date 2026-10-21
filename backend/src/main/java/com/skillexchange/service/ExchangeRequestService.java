package com.skillexchange.service;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.exchange.CreateExchangeRequestDto;
import com.skillexchange.dto.exchange.ExchangeRequestDto;
import com.skillexchange.entity.ExchangeRequest;
import com.skillexchange.entity.Skill;
import com.skillexchange.entity.User;
import com.skillexchange.entity.ChatConnection;
import com.skillexchange.entity.enums.ConnectionStatus;
import com.skillexchange.entity.enums.ExchangeStatus;
import com.skillexchange.entity.enums.NotificationType;
import com.skillexchange.exception.AccessDeniedCustomException;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.ExchangeRequestRepository;
import com.skillexchange.repository.SkillRepository;
import com.skillexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExchangeRequestService {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final MatchingService matchingService;
    private final NotificationService notificationService;
    private final EntityMapper mapper;
    private final com.skillexchange.repository.UserSkillRepository userSkillRepository;
    private final com.skillexchange.repository.ChatConnectionRepository chatConnectionRepository;

    @Transactional
    public ExchangeRequestDto create(Long senderId, CreateExchangeRequestDto request) {
        // Business rule: cannot send a request to yourself
        if (senderId.equals(request.getReceiverId())) {
            throw new BusinessRuleException("You cannot send an exchange request to yourself");
        }
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        // Business rule: suspended users cannot create exchanges
        if (sender.isSuspended()) {
            throw new BusinessRuleException("Suspended users cannot create exchange requests");
        }
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));
        if (receiver.isSuspended()) {
            throw new BusinessRuleException("Cannot send a request to a suspended user");
        }
        Skill offeredSkill = skillRepository.findById(request.getOfferedSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Offered skill not found"));
        Skill requestedSkill = skillRepository.findById(request.getRequestedSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Requested skill not found"));
        if (!offeredSkill.isActive() || !requestedSkill.isActive()) {
            throw new BusinessRuleException("Exchange requests must use active skills");
        }
        if (!userSkillRepository.existsByUserIdAndSkillIdAndSkillType(senderId, offeredSkill.getId(), com.skillexchange.entity.enums.SkillType.TEACH)) {
            throw new BusinessRuleException("You can only offer a skill that is in your teaching list");
        }
        if (!userSkillRepository.existsByUserIdAndSkillIdAndSkillType(senderId, requestedSkill.getId(), com.skillexchange.entity.enums.SkillType.LEARN)) {
            throw new BusinessRuleException("You can only request a skill that is in your learning list");
        }
        if (exchangeRequestRepository.existsBySenderIdAndReceiverIdAndStatus(senderId, receiver.getId(), ExchangeStatus.PENDING)) {
            throw new BusinessRuleException("You already have a pending exchange request with this user");
        }

        Double matchPercentage = matchingService.calculateMatchBetween(senderId, receiver.getId())
                .getOverallMatchPercentage();

        ExchangeRequest exchangeRequest = ExchangeRequest.builder()
                .sender(sender).receiver(receiver)
                .offeredSkill(offeredSkill).requestedSkill(requestedSkill)
                .durationHours(request.getDurationHours())
                .message(request.getMessage())
                .status(ExchangeStatus.PENDING)
                .matchPercentage(matchPercentage)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        exchangeRequest = exchangeRequestRepository.save(exchangeRequest);

        notificationService.notify(receiver.getId(), NotificationType.EXCHANGE_REQUEST_RECEIVED,
                sender.getUsername() + " sent you an exchange request for " + requestedSkill.getName(),
                "/exchange-requests/" + exchangeRequest.getId());

        return mapper.toDto(exchangeRequest);
    }

    @Transactional
    public ExchangeRequestDto respond(Long userId, Long requestId, boolean accept) {
        ExchangeRequest exchangeRequest = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found"));
        if (!exchangeRequest.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedCustomException("Only the receiver can respond to this request");
        }
        if (exchangeRequest.getStatus() != ExchangeStatus.PENDING) {
            throw new BusinessRuleException("This request has already been " + exchangeRequest.getStatus().name().toLowerCase());
        }
        if (exchangeRequest.getExpiresAt() != null && exchangeRequest.getExpiresAt().isBefore(LocalDateTime.now())) {
            exchangeRequest.setStatus(ExchangeStatus.EXPIRED);
            exchangeRequestRepository.save(exchangeRequest);
            throw new BusinessRuleException("This exchange request has expired");
        }
        if (accept && (exchangeRequest.getSender().isSuspended() || exchangeRequest.getReceiver().isSuspended())) {
            throw new BusinessRuleException("A suspended participant cannot activate an exchange");
        }
        exchangeRequest.setStatus(accept ? ExchangeStatus.ACCEPTED : ExchangeStatus.REJECTED);
        exchangeRequestRepository.save(exchangeRequest);
        if (accept) {
            Long firstId = Math.min(exchangeRequest.getSender().getId(), exchangeRequest.getReceiver().getId());
            Long secondId = Math.max(exchangeRequest.getSender().getId(), exchangeRequest.getReceiver().getId());
            ChatConnection connection = chatConnectionRepository.findConnectionBetween(firstId, secondId)
                    .orElseGet(() -> ChatConnection.builder()
                            .user1(exchangeRequest.getSender().getId().equals(firstId) ? exchangeRequest.getSender() : exchangeRequest.getReceiver())
                            .user2(exchangeRequest.getSender().getId().equals(firstId) ? exchangeRequest.getReceiver() : exchangeRequest.getSender())
                            .status(ConnectionStatus.ACCEPTED)
                            .actionUser(exchangeRequest.getReceiver())
                            .build());
            connection.setStatus(ConnectionStatus.ACCEPTED);
            connection.setActionUser(exchangeRequest.getReceiver());
            chatConnectionRepository.save(connection);
        }

        notificationService.notify(exchangeRequest.getSender().getId(),
                accept ? NotificationType.EXCHANGE_REQUEST_ACCEPTED : NotificationType.EXCHANGE_REQUEST_REJECTED,
                exchangeRequest.getReceiver().getUsername() + " " + (accept ? "accepted" : "declined")
                        + " your exchange request",
                "/exchange-requests/" + exchangeRequest.getId());

        return mapper.toDto(exchangeRequest);
    }

    @Transactional
    public ExchangeRequestDto cancel(Long userId, Long requestId) {
        ExchangeRequest exchangeRequest = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found"));
        if (!exchangeRequest.getSender().getId().equals(userId) && !exchangeRequest.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedCustomException("You are not a participant in this exchange request");
        }
        if (exchangeRequest.getStatus() == ExchangeStatus.COMPLETED) {
            throw new BusinessRuleException("A completed exchange cannot be cancelled");
        }
        if (exchangeRequest.getStatus() == ExchangeStatus.REJECTED || exchangeRequest.getStatus() == ExchangeStatus.EXPIRED || exchangeRequest.getStatus() == ExchangeStatus.CANCELLED) {
            throw new BusinessRuleException("This exchange request is already closed");
        }
        exchangeRequest.setStatus(ExchangeStatus.CANCELLED);
        return mapper.toDto(exchangeRequestRepository.save(exchangeRequest));
    }

    @Transactional(readOnly = true)
    public Page<ExchangeRequestDto> getMyRequests(Long userId, Pageable pageable) {
        return exchangeRequestRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId, pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public ExchangeRequestDto getById(Long userId, Long id) {
        ExchangeRequest er = exchangeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange request not found"));
        if (!er.getSender().getId().equals(userId) && !er.getReceiver().getId().equals(userId)) {
            throw new AccessDeniedCustomException("You are not a participant in this exchange request");
        }
        return mapper.toDto(er);
    }
}
