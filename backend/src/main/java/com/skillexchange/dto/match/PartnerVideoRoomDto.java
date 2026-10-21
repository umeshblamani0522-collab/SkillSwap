package com.skillexchange.dto.match;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerVideoRoomDto {
    private String roomId;
    private String jitsiRoomName;
    private Long partnerId;
    private String partnerUsername;
    private LocalDateTime expiresAt;
    private String appId;
    private String jwt;
    private boolean useStaging;
}
