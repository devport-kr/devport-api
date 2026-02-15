package kr.devport.api.domain.wiki.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wiki chat response DTO.
 * Default responses are citation-light; citations included only when requested.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiChatResponse {

    private String answer;
    
    private boolean isClarification;
    
    private String sessionId;
    
    @Builder.Default
    private List<Citation> citations = List.of();

    /**
     * Citation for answer grounding (optional).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private String section;
        private String excerpt;
        private String relevance;
    }
}
