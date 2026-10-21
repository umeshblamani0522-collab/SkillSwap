package com.skillexchange.websocket;

import com.skillexchange.dto.chat.MessageDto;
import com.skillexchange.dto.chat.SendMessageRequest;
import com.skillexchange.repository.UserRepository;
import com.skillexchange.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * STOMP entry point for real-time chat.
 * Clients CONNECT with an Authorization: Bearer <jwt> native header (see
 * WebSocketAuthInterceptor), then SEND to /app/chat.send and SUBSCRIBE to
 * /user/queue/messages to receive messages destined for them, and
 * /app/chat.typing / /user/queue/typing for a typing indicator.
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void send(SendMessageRequest request, Principal principal) {
        Long senderId = resolveUserId(principal);
        chatService.sendMessage(senderId, request); // this also pushes to /user/queue/messages internally
    }

    @MessageMapping("/chat.typing")
    public void typing(Map<String, Object> payload, Principal principal) {
        Long senderId = resolveUserId(principal);
        Object receiverIdObj = payload.get("receiverId");
        if (receiverIdObj == null) return;
        Long receiverId;
        try { receiverId = Long.valueOf(receiverIdObj.toString()); }
        catch (NumberFormatException ex) { return; }
        if (senderId.equals(receiverId)) return;
        if (chatService.getConnectionStatus(senderId, receiverId) != com.skillexchange.entity.enums.ConnectionStatus.ACCEPTED) return;

        String senderUsername = userRepository.findById(senderId).map(u -> u.getUsername()).orElse("unknown");
        String receiverUsername = userRepository.findById(receiverId).map(u -> u.getUsername()).orElse(null);
        if (receiverUsername == null) return;

        messagingTemplate.convertAndSendToUser(receiverUsername, "/queue/typing",
                Map.of("fromUserId", senderId, "fromUsername", senderUsername, "typing", payload.getOrDefault("typing", true)));
    }

    private Long resolveUserId(Principal principal) {
        String username = principal.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Unknown WebSocket principal: " + username))
                .getId();
    }
}
