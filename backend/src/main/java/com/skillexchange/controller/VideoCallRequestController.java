package com.skillexchange.controller;

import com.skillexchange.dto.PageResponse;
import com.skillexchange.dto.video.VideoCallRequestDto;
import com.skillexchange.security.CurrentUser;
import com.skillexchange.service.VideoCallRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video-requests")
@RequiredArgsConstructor
public class VideoCallRequestController {
    private final VideoCallRequestService service;

    @PostMapping("/partner/{partnerId}")
    public ResponseEntity<VideoCallRequestDto> create(@PathVariable Long partnerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(CurrentUser.id(), partnerId));
    }

    @PostMapping("/session/{sessionId}")
    public ResponseEntity<VideoCallRequestDto> createForSession(@PathVariable Long sessionId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createForSession(CurrentUser.id(), sessionId));
    }

    @GetMapping("/incoming")
    public ResponseEntity<PageResponse<VideoCallRequestDto>> incoming(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.incoming(CurrentUser.id(), page, Math.min(size, 50)));
    }

    @GetMapping
    public ResponseEntity<PageResponse<VideoCallRequestDto>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.mine(CurrentUser.id(), page, Math.min(size, 50)));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<VideoCallRequestDto> accept(@PathVariable Long id) {
        return ResponseEntity.ok(service.accept(CurrentUser.id(), id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<VideoCallRequestDto> reject(@PathVariable Long id) {
        return ResponseEntity.ok(service.reject(CurrentUser.id(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<VideoCallRequestDto> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(CurrentUser.id(), id));
    }
}
