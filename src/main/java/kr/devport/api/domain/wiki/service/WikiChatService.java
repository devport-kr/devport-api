package kr.devport.api.domain.wiki.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore.ChatTurn;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Wiki chat service with uncertainty handling and session-scoped memory.
 * Returns clarifying questions when confidence is low instead of fabricated answers.
 * Session memory is short-lived and does not persist across sessions.
 */
@Service
@RequiredArgsConstructor
public class WikiChatService {

    private static final int MAX_PROMPT_TURNS = 2;
    private static final int MAX_CLARIFICATION_TURNS = 2;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9가-힣]+");

    private final WikiRetrievalService retrievalService;
    private final WikiChatSessionStore sessionStore;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        return chatResult(sessionId, projectExternalId, userQuestion).answer();
    }

    public WikiChatResult chatResult(String sessionId, String projectExternalId, String userQuestion) {
        WikiRetrievalContext context = retrievalService.retrieveContext(projectExternalId, userQuestion);
        boolean hadActiveSession = sessionStore.hasActiveSession(sessionId);
        List<ChatTurn> previousTurns = sessionStore.loadRecentTurns(sessionId, projectExternalId);
        boolean sessionReset = hadActiveSession && previousTurns.isEmpty();
        boolean topicShift = isTopicShift(previousTurns, userQuestion);
        List<ChatTurn> promptTurns = topicShift ? List.of() : selectPromptTurns(previousTurns);
        int clarificationTurns = countClarificationTurns(previousTurns);

        List<ChatCompletionMessageParam> messages = buildMessages(context, promptTurns, userQuestion, clarificationTurns);
        ChatCompletion completion = openAIClient.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_5_MINI)
                        .messages(messages)
                        .responseFormat(buildResponseFormat())
                        .build()
        );

        String payload = completion.choices().getFirst().message().content().orElse("");
        WikiChatResult result = normalizeResult(parseResult(payload, context), context, topicShift, sessionReset, clarificationTurns);

        sessionStore.saveTurn(sessionId, projectExternalId, userQuestion, result.answer(), result.isClarification());

        return result;
    }

    /**
     * Build chat messages with system prompt, context, history, and current question.
     */
    private List<ChatCompletionMessageParam> buildMessages(
            WikiRetrievalContext context,
            List<ChatTurn> previousTurns,
            String userQuestion,
            int clarificationTurns
    ) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();

        String systemPrompt = buildSystemPrompt(context, clarificationTurns > 0);
        messages.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(systemPrompt)
                        .build()
        ));

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

        messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(buildUserPrompt(context, userQuestion, clarificationTurns))
                        .build()
        ));

        return messages;
    }

    /**
     * Build system prompt with context and uncertainty handling instructions.
     */
    private String buildSystemPrompt(WikiRetrievalContext context, boolean hasPreviousContext) {
        return """
                You are a repository-grounded technical teammate.
                Output must be valid JSON matching the schema.
                Always write the answer in Korean only.

                Answer policy:
                - Start the answer with a short summary sentence.
                - Stay concise, direct, and repository-specific.
                - Mention file paths, classes, or methods only when grounded context supports them.
                - If the question is broad but partly answerable, answer the safest slice first and then add one narrow clarification.
                - If the question is ambiguous across repo areas, set isClarification=true and return 2-3 short clarificationOptions without question marks.
                - If grounding is weak, keep the answer short and action-oriented and include 2-3 suggestedNextQuestions.
                - Do not add confidence labels, citations, or generic chat filler.
                - usedPreviousContext should be true only when recent turns materially help this answer.

                Conversation policy:
                - Recent turns are already filtered to the most relevant context.
                - Do not repeat stale earlier topics unless they are clearly needed.
                - When previous clarification exists, continue naturally without looping.

                Grounding strength: %s
                Previous context included: %s

                Repository Context:
                %s
                """.formatted(context.weakGrounding() ? "weak" : "strong", hasPreviousContext, context.groundedContext());
    }

    private String buildUserPrompt(WikiRetrievalContext context, String userQuestion, int clarificationTurns) {
        return """
                질문: %s
                약한 근거 여부: %s
                기존 추천 질문: %s
                누적 clarification 턴: %d
                응답 JSON만 반환하세요.
                """.formatted(
                userQuestion,
                context.weakGrounding(),
                context.suggestedNextQuestions(),
                clarificationTurns
        );
    }

    private WikiChatResult parseResult(String payload, WikiRetrievalContext context) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            return new WikiChatResult(
                    root.path("answer").asText("").strip(),
                    root.path("isClarification").asBoolean(false),
                    readStringList(root.path("clarificationOptions")),
                    readStringList(root.path("suggestedNextQuestions")),
                    root.path("usedPreviousContext").asBoolean(false),
                    false
            );
        } catch (Exception ignored) {
            return fallbackResult(context);
        }
    }

    private WikiChatResult normalizeResult(
            WikiChatResult raw,
            WikiRetrievalContext context,
            boolean topicShift,
            boolean sessionReset,
            int clarificationTurns
    ) {
        String answer = raw.answer().isBlank() ? fallbackAnswer(context) : raw.answer();
        boolean isClarification = raw.isClarification();
        List<String> clarificationOptions = sanitizeList(raw.clarificationOptions());
        List<String> suggestedNextQuestions = sanitizeList(raw.suggestedNextQuestions());

        if (isClarification && clarificationOptions.isEmpty()) {
            clarificationOptions = deriveClarificationOptions(context);
        }
        if (context.weakGrounding() && suggestedNextQuestions.isEmpty()) {
            suggestedNextQuestions = sanitizeList(context.suggestedNextQuestions());
        }
        if (clarificationTurns >= MAX_CLARIFICATION_TURNS && isClarification) {
            isClarification = false;
            clarificationOptions = List.of();
            answer = fallbackAnswer(context);
        }

        return new WikiChatResult(
                answer,
                isClarification,
                clarificationOptions,
                suggestedNextQuestions,
                !topicShift && raw.usedPreviousContext(),
                sessionReset
        );
    }

    private WikiChatResult fallbackResult(WikiRetrievalContext context) {
        return new WikiChatResult(
                fallbackAnswer(context),
                false,
                List.of(),
                sanitizeList(context.suggestedNextQuestions()),
                false,
                false
        );
    }

    private String fallbackAnswer(WikiRetrievalContext context) {
        if (!context.chunks().isEmpty()) {
            var firstChunk = context.chunks().getFirst();
            String anchor = firstChunk.sourcePathHint() != null ? firstChunk.sourcePathHint() : firstChunk.heading();
            if (context.weakGrounding()) {
                return "요약하면 지금은 " + anchor + " 근거까지만 확인돼요.";
            }
            return "요약하면 핵심 흐름은 " + anchor + " 기준으로 보는 게 가장 안전해요.";
        }
        return "요약하면 지금 확보된 저장소 근거만으로는 좁은 범위부터 확인하는 게 안전해요.";
    }

    private List<ChatTurn> selectPromptTurns(List<ChatTurn> previousTurns) {
        if (previousTurns.size() <= MAX_PROMPT_TURNS) {
            return previousTurns;
        }
        return new ArrayList<>(previousTurns.subList(previousTurns.size() - MAX_PROMPT_TURNS, previousTurns.size()));
    }

    private int countClarificationTurns(List<ChatTurn> turns) {
        return (int) turns.stream().filter(ChatTurn::isWasClarification).count();
    }

    private boolean isTopicShift(List<ChatTurn> previousTurns, String userQuestion) {
        if (previousTurns.isEmpty()) {
            return false;
        }
        Set<String> currentTokens = tokenize(userQuestion);
        if (currentTokens.isEmpty()) {
            return false;
        }

        Set<String> previousTokens = new LinkedHashSet<>();
        for (ChatTurn turn : selectPromptTurns(previousTurns)) {
            previousTokens.addAll(tokenize(turn.getQuestion()));
            previousTokens.addAll(tokenize(turn.getAnswer()));
        }

        return currentTokens.stream().noneMatch(previousTokens::contains);
    }

    private Set<String> tokenize(String text) {
        return TOKEN_SPLIT.splitAsStream(text.toLowerCase(Locale.ROOT))
                .filter(token -> token.length() >= 2)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private List<String> readStringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String value = item.asText("").strip();
            if (!value.isBlank()) {
                values.add(value);
            }
        });
        return values;
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.strip();
            if (!trimmed.isBlank()) {
                sanitized.add(trimmed);
            }
            if (sanitized.size() == 3) {
                break;
            }
        }
        return List.copyOf(sanitized);
    }

    private List<String> deriveClarificationOptions(WikiRetrievalContext context) {
        LinkedHashSet<String> options = new LinkedHashSet<>();
        context.chunks().forEach(chunk -> {
            options.add(chunk.heading());
            if (chunk.sourcePathHint() != null) {
                options.add(chunk.sourcePathHint());
            }
        });
        return sanitizeList(new ArrayList<>(options));
    }

    private ResponseFormatJsonSchema buildResponseFormat() {
        var schema = ResponseFormatJsonSchema.JsonSchema.Schema.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(java.util.Map.of(
                        "answer", java.util.Map.of("type", "string"),
                        "isClarification", java.util.Map.of("type", "boolean"),
                        "clarificationOptions", java.util.Map.of("type", "array", "items", java.util.Map.of("type", "string")),
                        "suggestedNextQuestions", java.util.Map.of("type", "array", "items", java.util.Map.of("type", "string")),
                        "usedPreviousContext", java.util.Map.of("type", "boolean")
                )))
                .putAdditionalProperty("required", JsonValue.from(List.of(
                        "answer",
                        "isClarification",
                        "clarificationOptions",
                        "suggestedNextQuestions",
                        "usedPreviousContext"
                )))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build();

        return ResponseFormatJsonSchema.builder()
                .jsonSchema(ResponseFormatJsonSchema.JsonSchema.builder()
                        .name("wiki_chat_result")
                        .strict(true)
                        .schema(schema)
                        .build())
                .build();
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
