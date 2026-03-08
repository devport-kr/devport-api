package kr.devport.api.domain.wiki.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wiki chat response DTO.
 * Compact payload for right-rail chat module consumption.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiChatResponse {

    private String answer;

    @JsonProperty("isClarification")
    private boolean isClarification;

    private String sessionId;
}
