package kr.devport.api.domain.wiki.dto.response;

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
    
    private boolean isClarification;
    
    private String sessionId;
}
