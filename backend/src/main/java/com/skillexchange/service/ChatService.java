package com.skillexchange.service;

import com.skillexchange.dto.chat.MessageDto;
import com.skillexchange.dto.chat.SendMessageRequest;
import com.skillexchange.dto.UserProfileDto;
import com.skillexchange.entity.ChatConnection;
import com.skillexchange.entity.Message;
import com.skillexchange.entity.User;
import com.skillexchange.entity.enums.ConnectionStatus;
import com.skillexchange.entity.enums.NotificationType;
import com.skillexchange.exception.BusinessRuleException;
import com.skillexchange.exception.ResourceNotFoundException;
import com.skillexchange.mapper.EntityMapper;
import com.skillexchange.repository.ChatConnectionRepository;
import com.skillexchange.repository.MessageRepository;
import com.skillexchange.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final ChatConnectionRepository chatConnectionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EntityMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.skillexchange.service.UserService userService;

    @Transactional
    public MessageDto sendMessage(Long senderId, SendMessageRequest request) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        if (senderId.equals(receiver.getId())) {
            throw new BusinessRuleException("You cannot send a message to yourself");
        }
        ChatConnection connection = chatConnectionRepository.findConnectionBetween(sender.getId(), receiver.getId())
                .orElseThrow(() -> new BusinessRuleException("Messaging is available after an exchange request is accepted"));

        if (connection.getStatus() != ConnectionStatus.ACCEPTED) {
            throw new BusinessRuleException(connection.getStatus() == ConnectionStatus.BLOCKED
                    ? "You cannot send messages to this user"
                    : "Messaging is available after the connection is accepted");
        }

        Message message = Message.builder()
                .sender(sender).receiver(receiver)
                .content(request.getContent())
                .read(false)
                .build();
        message = messageRepository.save(message);
        MessageDto dto = mapper.toDto(message);

        messagingTemplate.convertAndSendToUser(receiver.getUsername(), "/queue/messages", dto);
        messagingTemplate.convertAndSendToUser(sender.getUsername(), "/queue/messages", dto);

        notificationService.notify(receiver.getId(), NotificationType.NEW_MESSAGE,
                sender.getUsername() + " sent you a message", "/chat/" + sender.getId());

        return dto;
    }

    @Transactional
    public ConnectionStatus updateConnectionStatus(Long userId, Long otherUserId, ConnectionStatus newStatus) {
        if (userId.equals(otherUserId)) {
            throw new BusinessRuleException("You cannot manage a connection with yourself");
        }
        ChatConnection connection = chatConnectionRepository.findConnectionBetween(userId, otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (newStatus == ConnectionStatus.ACCEPTED && connection.getStatus() == ConnectionStatus.PENDING
                && connection.getActionUser() != null && connection.getActionUser().getId().equals(userId)) {
            throw new BusinessRuleException("The user who initiated the connection cannot accept their own request");
        }
        connection.setStatus(newStatus);
        connection.setActionUser(me);
        chatConnectionRepository.save(connection);
        return newStatus;
    }

    @Transactional(readOnly = true)
    public ConnectionStatus getConnectionStatus(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) return ConnectionStatus.BLOCKED;
        return chatConnectionRepository.findConnectionBetween(userId, otherUserId)
                .map(ChatConnection::getStatus)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Long getActionUserId(Long userId, Long otherUserId) {
        return chatConnectionRepository.findConnectionBetween(userId, otherUserId)
                .map(c -> c.getActionUser().getId())
                .orElse(null);
    }

    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new BusinessRuleException("You can only delete your own messages");
        }

        Long receiverId = message.getReceiver().getId();
        String senderUsername = message.getSender().getUsername();
        String receiverUsername = message.getReceiver().getUsername();

        messageRepository.delete(message);
        messageRepository.flush();

        Map<String, Object> event = Map.of(
                "type", "MESSAGE_DELETED",
                "messageId", messageId
        );
        messagingTemplate.convertAndSendToUser(senderUsername, "/queue/messages", event);
        messagingTemplate.convertAndSendToUser(receiverUsername, "/queue/messages", event);
    }

    @Transactional(readOnly = true)
    public Page<MessageDto> getConversation(Long requestingUserId, Long otherUserId, Pageable pageable) {
        if (requestingUserId.equals(otherUserId)) {
            throw new BusinessRuleException("A conversation requires two different users");
        }
        userRepository.findById(otherUserId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ConnectionStatus status = getConnectionStatus(requestingUserId, otherUserId);
        if (status != ConnectionStatus.ACCEPTED) {
            throw new BusinessRuleException("This conversation is not available");
        }
        return messageRepository.findConversation(requestingUserId, otherUserId, pageable).map(mapper::toDto);
    }

    @Transactional
    public void markConversationRead(Long userId, Long otherUserId) {
        Page<MessageDto> page = getConversation(userId, otherUserId, org.springframework.data.domain.Pageable.unpaged());
        page.getContent().forEach(m -> {
            if (m.getReceiverId().equals(userId) && !m.isRead()) {
                Message entity = messageRepository.findById(m.getId()).orElse(null);
                if (entity != null) {
                    entity.setRead(true);
                    entity.setReadAt(LocalDateTime.now());
                    messageRepository.save(entity);
                }
            }
        });
    }

    public long unreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public java.util.List<UserProfileDto> listAcceptedConversations(Long userId) {
        return chatConnectionRepository.findConnectionsForUser(userId, ConnectionStatus.ACCEPTED).stream()
                .map(c -> c.getUser1().getId().equals(userId) ? c.getUser2().getId() : c.getUser1().getId())
                .map(userService::getPublicProfile)
                .toList();
    }
}
