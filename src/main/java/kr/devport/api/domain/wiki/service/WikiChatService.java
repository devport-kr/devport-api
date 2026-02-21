package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore.ChatTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Wiki chat service with uncertainty handling and session-scoped memory.
 * Returns clarifying questions when confidence is low instead of fabricated answers.
 * Session memory is short-lived and does not persist across sessions.
 */
@Service
@RequiredArgsConstructor
public class WikiChatService {

    private final WikiRetrievalService retrievalService;
    private final WikiChatSessionStore sessionStore;
    private final OpenAIClient openAIClient;

    /**
     * Generate chat response with grounded context and uncertainty handling.
     * When evidence is weak or ambiguous, returns a clarifying question.
     *
     * @param sessionId Session identifier for memory tracking
     * @param projectExternalId Project external ID
     * @param userQuestion User's question
     * @return Chat answer (or clarifying question if uncertain)
     */
    public String chat(String sessionId, String projectExternalId, String userQuestion) {
        // Retrieve grounded context
        String context = retrievalService.retrieveContext(projectExternalId, userQuestion);

        // Load session history
        List<ChatTurn> previousTurns = sessionStore.loadTurns(sessionId);

        // Build messages with context and history
        List<ChatCompletionMessageParam> messages = buildMessages(context, previousTurns, userQuestion);

        // Generate response
        ChatCompletion completion = openAIClient.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_5_MINI)
                        .messages(messages)
                        .temperature(0.1)
                        .build()
        );

        String answer = completion.choices().get(0).message().content().orElse("");

        // Check if response indicates uncertainty
        boolean isClarification = isUncertainResponse(answer);

        // Save turn to session
        sessionStore.saveTurn(sessionId, projectExternalId, userQuestion, answer, isClarification);

        return answer;
    }

    /**
     * Build chat messages with system prompt, context, history, and current question.
     */
    private List<ChatCompletionMessageParam> buildMessages(
            String context,
            List<ChatTurn> previousTurns,
            String userQuestion
    ) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();

        // System prompt with high-precision guidance
        String systemPrompt = buildSystemPrompt(context);
        messages.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(systemPrompt)
                        .build()
        ));

        // Add previous turns for context
        for (ChatTurn turn : previousTurns) {
            messages.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                            .content(turn.getQuestion())
                            .build()
            ));
            messages.add(ChatCompletionMessageParam.ofAssistant(
                    ChatCompletionAssistantMessageParam.builder()
                            .content(turn.getAnswer())
                            .build()
            ));
        }

        // Add current question
        messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(userQuestion)
                        .build()
        ));

        return messages;
    }

    /**
     * Build system prompt with context and uncertainty handling instructions.
     */
    private String buildSystemPrompt(String context) {
        return """
                You are a precise technical assistant for this repository.
                
                Your answers must be grounded in the provided repository context.
                When the context does not contain sufficient information to answer accurately:
                1. Acknowledge what you don't know
                2. Ask a clarifying question to narrow down the user's intent
                3. Never fabricate or guess information
                
                Repository Context:
                %s
                
                Guidelines:
                - Be concise and technical
                - Reference concrete files and functions from the repository context when relevant
                - If uncertain, ask for clarification instead of making assumptions
                - Focus on repository-specific details, not general ecosystem knowledge
                """.formatted(context);
    }

    /**
     * Detect if response indicates uncertainty or is a clarifying question.
     * Simple heuristic: check for question marks and uncertainty phrases.
     */
    private boolean isUncertainResponse(String response) {
        String lower = response.toLowerCase();
        return response.contains("?") ||
               lower.contains("could you clarify") ||
               lower.contains("can you specify") ||
               lower.contains("what do you mean") ||
               lower.contains("i don't have") ||
               lower.contains("not sure") ||
               lower.contains("unclear");
    }

    /**
     * Clear session memory explicitly.
     *
     * @param sessionId Session identifier
     */
    public void clearSession(String sessionId) {
        sessionStore.clearSession(sessionId);
    }

    /**
     * Check if session has active memory.
     *
     * @param sessionId Session identifier
     * @return true if session exists and is not expired
     */
    public boolean hasActiveSession(String sessionId) {
        return sessionStore.hasActiveSession(sessionId);
    }
}
