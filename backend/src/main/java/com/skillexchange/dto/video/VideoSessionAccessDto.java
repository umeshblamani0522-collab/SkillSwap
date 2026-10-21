package com.skillexchange.dto.video;

import com.skillexchange.entity.enums.SessionStatus;

public record VideoSessionAccessDto(
        Long sessionId,
        SessionStatus status,
        String meetingInfo,
        Long participantId,
        String username,
        String fullName,
        String email,
        boolean moderator
) {}
