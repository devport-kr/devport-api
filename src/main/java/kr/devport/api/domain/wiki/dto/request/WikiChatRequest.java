package kr.devport.api.domain.wiki.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wiki chat request DTO.
 * Compact payload for right-rail chat module consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiChatRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 1000, message = "Question must not exceed 1000 characters")
    private String question;

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @Builder.Default
    private boolean includeCitations = false;
}
