package kr.devport.api.domain.wiki.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
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

    public static WikiChatResponse from(WikiChatResult result, String sessionId) {
        return WikiChatResponse.builder()
                .answer(result.answer())
                .isClarification(result.isClarification())
                .sessionId(sessionId)
                .build();
    }
}
