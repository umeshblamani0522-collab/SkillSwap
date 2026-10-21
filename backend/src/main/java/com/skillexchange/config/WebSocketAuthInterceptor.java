package com.skillexchange.config;

import com.skillexchange.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Authenticates every STOMP session at CONNECT time. A client without a valid
 * bearer token is rejected before it can subscribe to user destinations.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessagingException("WebSocket authentication required");
            }
            try {
                String token = authHeader.substring(7).trim();
                String username = jwtUtil.extractUsername(token);
                if (username == null || !jwtUtil.isTokenValid(token, username)) {
                    throw new MessagingException("Invalid WebSocket authentication token");
                }
                Principal principal = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username, null, java.util.List.of());
                accessor.setUser(principal);
            } catch (MessagingException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new MessagingException("Invalid WebSocket authentication token", ex);
            }
        }
        return message;
    }
}
