package com.skillexchange.controller;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.chat.MessageDto;
import com.skillexchange.dto.chat.SendMessageRequest;
import com.skillexchange.entity.enums.ConnectionStatus;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/messages")
    public ResponseEntity<MessageDto> send(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendMessage(CurrentUser.id(), request));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        chatService.deleteMessage(CurrentUser.id(), messageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conversations/{otherUserId}")
    public ResponseEntity<PageResponse<MessageDto>> conversation(
            @PathVariable Long otherUserId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(PageResponse.of(chatService.getConversation(CurrentUser.id(), otherUserId,
                PageRequest.of(page, size, Sort.by("createdAt").ascending()))));
    }

    @PostMapping("/conversations/{otherUserId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long otherUserId) {
        chatService.markConversationRead(CurrentUser.id(), otherUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conversations")
    public ResponseEntity<java.util.List<com.skillexchange.dto.UserProfileDto>> conversations() {
        return ResponseEntity.ok(chatService.listAcceptedConversations(CurrentUser.id()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount() {
        return ResponseEntity.ok(chatService.unreadCount(CurrentUser.id()));
    }

    @GetMapping("/conversations/{otherUserId}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long otherUserId) {
        ConnectionStatus status = chatService.getConnectionStatus(CurrentUser.id(), otherUserId);
        Long actionUserId = chatService.getActionUserId(CurrentUser.id(), otherUserId);
        return ResponseEntity.ok(Map.of(
            "status", status == null ? "NONE" : status.name(),
            "actionUserId", actionUserId == null ? 0 : actionUserId
        ));
    }

    @PostMapping("/conversations/{otherUserId}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long otherUserId) {
        chatService.updateConnectionStatus(CurrentUser.id(), otherUserId, ConnectionStatus.ACCEPTED);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conversations/{otherUserId}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long otherUserId) {
        chatService.updateConnectionStatus(CurrentUser.id(), otherUserId, ConnectionStatus.BLOCKED);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conversations/{otherUserId}/block")
    public ResponseEntity<Void> block(@PathVariable Long otherUserId) {
        chatService.updateConnectionStatus(CurrentUser.id(), otherUserId, ConnectionStatus.BLOCKED);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/conversations/{otherUserId}/unblock")
    public ResponseEntity<Void> unblock(@PathVariable Long otherUserId) {
        chatService.updateConnectionStatus(CurrentUser.id(), otherUserId, ConnectionStatus.ACCEPTED);
        return ResponseEntity.ok().build();
    }
}
