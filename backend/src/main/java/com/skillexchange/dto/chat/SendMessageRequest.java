package com.skillexchange.dto.chat;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SendMessageRequest {
    @NotNull
    private Long receiverId;
    @NotBlank @Size(max = 2000)
    private String content;
}
