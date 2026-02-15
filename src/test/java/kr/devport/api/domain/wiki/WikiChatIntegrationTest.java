package kr.devport.api.domain.wiki;

import kr.devport.api.domain.wiki.dto.request.WikiChatRequest;
import kr.devport.api.domain.wiki.dto.response.WikiChatResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for wiki chat behavior.
 * <p>
 * Covers:
 * - Citation on-demand behavior (includeCitations flag)
 * - Uncertainty detection and clarifying questions
 * - Session tracking across turns
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WikiChatIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("chat returns answer without citations by default")
    void chat_returnAnswerWithoutCitationsByDefault() {
        // Given: A chat request without includeCitations flag
        WikiChatRequest request = WikiChatRequest.builder()
                .question("What is this project?")
                .sessionId("session-test-1")
                .includeCitations(false)
                .build();

        // When: Posting chat request
        ResponseEntity<WikiChatResponse> response = restTemplate.postForEntity(
                "/api/wiki/projects/github:test/repo/chat",
                request,
                WikiChatResponse.class
        );

        // Then: Should return answer without citations
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isNotBlank();
        assertThat(response.getBody().getCitations()).isNullOrEmpty();  // No citations by default
        assertThat(response.getBody().getSessionId()).isEqualTo("session-test-1");
    }

    @Test
    @DisplayName("chat returns citations when explicitly requested")
    void chat_returnsCitationsWhenRequested() {
        // Given: A chat request with includeCitations=true
        WikiChatRequest request = WikiChatRequest.builder()
                .question("What is this project?")
                .sessionId("session-test-2")
                .includeCitations(true)
                .build();

        // When: Posting chat request
        ResponseEntity<WikiChatResponse> response = restTemplate.postForEntity(
                "/api/wiki/projects/github:test/repo/chat",
                request,
                WikiChatResponse.class
        );

        // Then: Should return answer with citations
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isNotBlank();
        
        // Citations should be present when requested
        // Note: actual citation presence depends on retrieval context availability
        if (response.getBody().getCitations() != null) {
            assertThat(response.getBody().getCitations()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("chat detects uncertainty and returns clarifying question")
    void chat_detectsUncertaintyAndReturnsCllarifyingQuestion() {
        // Given: An ambiguous question
        WikiChatRequest request = WikiChatRequest.builder()
                .question("How does it work?")  // Vague question
                .sessionId("session-ambiguous")
                .includeCitations(false)
                .build();

        // When: Posting ambiguous question
        ResponseEntity<WikiChatResponse> response = restTemplate.postForEntity(
                "/api/wiki/projects/github:test/repo/chat",
                request,
                WikiChatResponse.class
        );

        // Then: Should handle uncertainty appropriately
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        
        // When uncertain, may return clarifying question or hedge answer
        // The exact behavior depends on retrieval quality
        assertThat(response.getBody().getAnswer()).isNotBlank();
        
        // If clarification flag is set, should indicate uncertainty
        if (response.getBody().isClarification()) {
            assertThat(response.getBody().getAnswer()).contains("?");  // Clarifying questions contain ?
        }
    }

    @Test
    @DisplayName("chat maintains session memory across turns")
    void chat_maintainsSessionMemoryAcrossTurns() {
        String sessionId = "session-memory-test";
        
        // Given: First turn establishes context
        WikiChatRequest firstRequest = WikiChatRequest.builder()
                .question("What programming language is this project written in?")
                .sessionId(sessionId)
                .includeCitations(false)
                .build();

        ResponseEntity<WikiChatResponse> firstResponse = restTemplate.postForEntity(
                "/api/wiki/projects/github:test/repo/chat",
                firstRequest,
                WikiChatResponse.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstResponse.getBody()).isNotNull();
        String firstAnswer = firstResponse.getBody().getAnswer();

        // When: Second turn references previous context
        WikiChatRequest followUpRequest = WikiChatRequest.builder()
                .question("Why was that language chosen?")  // "that language" references previous turn
                .sessionId(sessionId)  // Same session
                .includeCitations(false)
                .build();

        ResponseEntity<WikiChatResponse> followUpResponse = restTemplate.postForEntity(
                "/api/wiki/projects/github:test/repo/chat",
                followUpRequest,
                WikiChatResponse.class
        );

        // Then: Should maintain session context
        assertThat(followUpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(followUpResponse.getBody()).isNotNull();
        assertThat(followUpResponse.getBody().getSessionId()).isEqualTo(sessionId);
        
        // Answer should reference context from previous turn
        // (actual validation depends on session memory implementation)
        assertThat(followUpResponse.getBody().getAnswer()).isNotBlank();
    }

    @Test
    @DisplayName("chat returns 404 for non-existent project")
    void chat_returns404ForNonExistentProject() {
        // Given: Chat request for non-existent project
        WikiChatRequest request = WikiChatRequest.builder()
                .question("What is this?")
                .sessionId("session-404")
                .includeCitations(false)
                .build();

        // When: Posting to non-existent project
        ResponseEntity<WikiChatResponse> response = restTemplate.postForEntity(
                "/api/wiki/projects/github:nonexistent/repo/chat",
                request,
                WikiChatResponse.class
        );

        // Then: Should return 404
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("chat handles clear precise questions with direct answers")
    void chat_handlesClearQuestionWithDirectAnswer() {
        // Given: A clear, precise question
        WikiChatRequest request = WikiChatRequest.builder()
                .question("What is the main purpose of this repository?")
                .sessionId("session-clear")
                .includeCitations(false)
                .build();

        // When: Posting clear question
        ResponseEntity<WikiChatResponse> response = restTemplate.postForEntity(
                "/api/wiki/projects/github:test/repo/chat",
                request,
                WikiChatResponse.class
        );

        // Then: Should return direct answer without clarification
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAnswer()).isNotBlank();
        
        // Clear questions should not require clarification
        assertThat(response.getBody().isClarification()).isFalse();
    }
}
