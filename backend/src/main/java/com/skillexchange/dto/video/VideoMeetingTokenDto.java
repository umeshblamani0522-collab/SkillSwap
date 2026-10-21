package com.skillexchange.dto.video;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoMeetingTokenDto {
    private String appId;
    private String roomName;
    private String jwt;
    private boolean useStaging;
}
