package kr.devport.api.domain.wiki.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.core.http.StreamResponse;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.wiki.dto.internal.WikiChatResult;
import kr.devport.api.domain.wiki.enums.WikiChatSessionType;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore.ChatTurn;
import kr.devport.api.domain.wiki.dto.internal.WikiRetrievalContext;
import kr.devport.api.domain.wiki.entity.WikiChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Wiki chat service with uncertainty handling and session-scoped memory.
 * Returns clarifying questions when confidence is low instead of fabricated answers.
 * Session memory is short-lived and does not persist across sessions.
 */
@Service
@RequiredArgsConstructor
public class WikiChatService {

    private static final int MAX_PROMPT_TURNS = 10;
    private static final int MAX_CLARIFICATION_TURNS = 2;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9가-힣]+");
    private static final String CLARIFICATION_HEADING = "선택할 수 있는 범위:";
    private static final String SUGGESTED_QUESTION_HEADING = "다음처럼 좁혀서 물어보면 더 정확해요:";
    private static final Pattern META_HISTORY_PATTERN = Pattern.compile(
        "이전|예전|방금|아까|요약|정리|다시|뭐라고|말했|물어봤" +
        "|before|previous|summary|summarize|earlier|what did",
        Pattern.CASE_INSENSITIVE
    );

    private final WikiRetrievalService retrievalService;
    private final WikiChatSessionStore sessionStore;
    private final WikiChatSessionPersistenceService persistenceService;
    private final WikiChatTitleService titleService;
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
        return chatResult(sessionId, projectExternalId, userQuestion, null).answer();
    }

    public WikiChatResult chatResult(String sessionId, String projectExternalId, String userQuestion) {
        return chatResult(sessionId, projectExternalId, userQuestion, null);
    }

    public WikiChatResult chatResult(String sessionId, String projectExternalId, String userQuestion, User user) {
        ChatRequestContext chatRequest = prepareChatRequest(sessionId, projectExternalId, userQuestion, user);
        List<ChatCompletionMessageParam> messages = buildMessages(
                chatRequest.context(),
                chatRequest.promptTurns(),
                userQuestion,
                chatRequest.clarificationTurns(),
                false
        );
        ChatCompletion completion = openAIClient.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_5_MINI)
                        .messages(messages)
                        .responseFormat(buildResponseFormat())
                        .build()
        );

        String payload = completion.choices().getFirst().message().content().orElse("");
        WikiChatResult result = normalizeResult(
                parseResult(payload, chatRequest.context()),
                chatRequest.context(),
                chatRequest.topicShift(),
                chatRequest.sessionReset(),
                chatRequest.clarificationTurns()
        );

        if (user != null) {
            boolean isFirst = persistenceService.isFirstMessage(sessionId);
            WikiChatSession session = persistenceService.findOrCreateSession(
                    sessionId, user, projectExternalId, WikiChatSessionType.PROJECT);
            persistenceService.saveUserMessage(session, userQuestion);
            persistenceService.saveAssistantMessage(session, result.answer(), result.isClarification());
            if (isFirst) {
                titleService.generateAndSave(sessionId, userQuestion);
            }
        }
        sessionStore.saveTurn(sessionId, projectExternalId, userQuestion, result.answer(), result.isClarification());

        return result;
    }

    public WikiChatResult streamChatResult(
            String sessionId,
            String projectExternalId,
            String userQuestion,
            Consumer<String> tokenConsumer
    ) {
        return streamChatResult(sessionId, projectExternalId, userQuestion, tokenConsumer, null);
    }

    public WikiChatResult streamChatResult(
            String sessionId,
            String projectExternalId,
            String userQuestion,
            Consumer<String> tokenConsumer,
            User user
    ) {
        ChatRequestContext chatRequest = prepareChatRequest(sessionId, projectExternalId, userQuestion, user);
        List<ChatCompletionMessageParam> messages = buildMessages(
                chatRequest.context(),
                chatRequest.promptTurns(),
                userQuestion,
                chatRequest.clarificationTurns(),
                true
        );
        StringBuilder accumulated = new StringBuilder();

        try (StreamResponse<ChatCompletionChunk> completionStream = openAIClient.chat().completions().createStreaming(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_5_MINI)
                        .messages(messages)
                        .build()
        );
             java.util.stream.Stream<ChatCompletionChunk> chunks = completionStream.stream()) {
            chunks.forEach(chunk -> appendStreamChunk(chunk, tokenConsumer, accumulated));
        }

        WikiChatResult result = normalizeStreamedResult(
                accumulated.toString(),
                chatRequest,
                !chatRequest.topicShift() && !chatRequest.promptTurns().isEmpty()
        );

        if (user != null) {
            boolean isFirst = persistenceService.isFirstMessage(sessionId);
            WikiChatSession session = persistenceService.findOrCreateSession(
                    sessionId, user, projectExternalId, WikiChatSessionType.PROJECT);
            persistenceService.saveUserMessage(session, userQuestion);
            persistenceService.saveAssistantMessage(session, result.answer(), result.isClarification());
            if (isFirst) {
                titleService.generateAndSave(sessionId, userQuestion);
            }
        }
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
            int clarificationTurns,
            boolean streaming
    ) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();

        String systemPrompt = buildSystemPrompt(context, clarificationTurns > 0, streaming);
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
                        .content(buildUserPrompt(context, userQuestion, clarificationTurns, streaming))
                        .build()
        ));

        return messages;
    }

    /**
     * Build system prompt with context and uncertainty handling instructions.
     */
    private String buildSystemPrompt(WikiRetrievalContext context, boolean hasPreviousContext, boolean streaming) {
        if (streaming) {
            return """
                You are a repository-grounded technical teammate.
                Output in rich, readable Markdown format.
                Do not output JSON.
                Always write the answer in Korean only.

                Markdown Formatting Policy:
                - Use fenced code blocks with language identifiers (e.g., ```java, ```bash) for multi-line code snippets or command outputs.
                - Use single backticks (`) for inline code, variables, or file paths.
                - Use bolding (**text**) for emphasis on key technical terms.
                - Use bullet points (-) or numbered lists to break down complex explanations or steps.
                - You MUST insert an empty line between every bullet point, numbered list item, and paragraph to ensure extreme readability.

                Answer policy:
                - Stay concise, direct, and repository-specific.
                - Mention file paths, classes, or methods only when grounded context supports them.
                - If the question is broad but partly answerable, answer the safest slice first and then add one narrow clarification.
                - If the question is ambiguous across repo areas, write %s on its own line and follow it with 2-3 short - bullet options without question marks.
                - If grounding is weak, keep the answer short and action-oriented, then write %s on its own line and follow it with 2-3 short - bullet suggestions.
                - Do not add confidence labels, citations, JSON, or generic chat filler.

                Conversation policy:
                - Recent turns are already filtered to the most relevant context.
                - Do not repeat stale earlier topics unless they are clearly needed.
                - When previous clarification exists, continue naturally without looping.

                Grounding strength: %s
                Previous context included: %s

                Repository Context:
                %s
                """.formatted(
                    CLARIFICATION_HEADING,
                    SUGGESTED_QUESTION_HEADING,
                    context.weakGrounding() ? "weak" : "strong",
                    hasPreviousContext,
                    context.groundedContext()
            );
        }

        return """
                You are a repository-grounded technical teammate.
                Output must be valid JSON matching the schema.
                Format the answer field in rich, readable Markdown.
                Always write the answer in Korean only.

                Markdown Formatting Policy:
                - Use fenced code blocks with language identifiers (e.g., ```java, ```bash) for multi-line code snippets or command outputs.
                - Use single backticks (`) for inline code, variables, or file paths.
                - Use bolding (**text**) for emphasis on key technical terms.
                - Use bullet points (-) or numbered lists to break down complex explanations or steps.
                - You MUST insert an empty line between every bullet point, numbered list item, and paragraph to ensure extreme readability.

                Answer policy:
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

    private String buildUserPrompt(
            WikiRetrievalContext context,
            String userQuestion,
            int clarificationTurns,
            boolean streaming
    ) {
        String responseInstruction = streaming
                ? "응답은 마크다운(Markdown)이 적용된 한국어 텍스트로 반환하세요."
                : "응답 JSON만 반환하세요.";
        String faqInstruction = buildFaqInstruction(userQuestion);
        return """
                질문: %s
                약한 근거 여부: %s
                기존 추천 질문: %s
                누적 clarification 턴: %d
                %s
                %s
                """.formatted(
                userQuestion,
                context.weakGrounding(),
                context.suggestedNextQuestions(),
                clarificationTurns,
                faqInstruction,
                responseInstruction
        );
    }

    private static final java.util.regex.Pattern FAQ_PROBLEM  = java.util.regex.Pattern.compile(
            "문제.*해결|해결.*문제|왜 만들|어떤.*목적|what.*problem|solve|why.*built", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern FAQ_ARCH     = java.util.regex.Pattern.compile(
            "아키텍처|구조|설계|architecture|design|어떻게.*동작|동작.*원리|how.*work|내부.*구조", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern FAQ_START    = java.util.regex.Pattern.compile(
            "시작하려면|어떻게.*시작|설치|install|setup|getting.?started|사용.*방법|how.*use|how.*start", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern FAQ_CHANGES  = java.util.regex.Pattern.compile(
            "최근.*변경|변경.*사항|changelog|release|업데이트|최근.*업|새로운|recent.*change|what.*new", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern FAQ_FEATURES = java.util.regex.Pattern.compile(
            "주요.*기능|기능.*무엇|특징|feature|capabilities|what.*can|뭘.*할|어떤.*기능", java.util.regex.Pattern.CASE_INSENSITIVE);

    private String buildFaqInstruction(String question) {
        if (FAQ_PROBLEM.matcher(question).find())
            return "FAQ 유형: 프로젝트 목적/문제 해결. 명확화 질문 없이 이 프로젝트가 해결하는 핵심 문제와 존재 이유를 포괄적으로 답하세요.";
        if (FAQ_ARCH.matcher(question).find())
            return "FAQ 유형: 핵심 아키텍처. 명확화 질문 없이 전체 시스템 구조, 주요 컴포넌트, 데이터 흐름을 구조화하여 설명하세요.";
        if (FAQ_START.matcher(question).find())
            return "FAQ 유형: 시작 방법. 명확화 질문 없이 설치/설정/첫 실행까지의 단계를 순서대로 안내하세요.";
        if (FAQ_CHANGES.matcher(question).find())
            return "FAQ 유형: 최근 변경 사항. 명확화 질문 없이 최근 업데이트, 릴리즈, 주요 변경 내용을 정리해 답하세요.";
        if (FAQ_FEATURES.matcher(question).find())
            return "FAQ 유형: 주요 기능. 명확화 질문 없이 핵심 기능 목록을 간결하게 나열하여 설명하세요.";
        return "";
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

        answer = enrichAnswer(answer, isClarification, clarificationOptions, context.weakGrounding(), suggestedNextQuestions);

        return new WikiChatResult(
                answer,
                isClarification,
                clarificationOptions,
                suggestedNextQuestions,
                !topicShift && raw.usedPreviousContext(),
                sessionReset
        );
    }

    private WikiChatResult normalizeStreamedResult(
            String accumulatedAnswer,
            ChatRequestContext chatRequest,
            boolean usedPreviousContext
    ) {
        return normalizeResult(
                new WikiChatResult(
                        accumulatedAnswer.strip(),
                        accumulatedAnswer.contains(CLARIFICATION_HEADING),
                        extractBulletList(accumulatedAnswer, CLARIFICATION_HEADING),
                        extractBulletList(accumulatedAnswer, SUGGESTED_QUESTION_HEADING),
                        usedPreviousContext,
                        false
                ),
                chatRequest.context(),
                chatRequest.topicShift(),
                chatRequest.sessionReset(),
                chatRequest.clarificationTurns()
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
        // Meta-history questions reference the conversation itself — never drop context.
        if (META_HISTORY_PATTERN.matcher(userQuestion).find()) {
            return false;
        }
        Set<String> currentTokens = tokenize(userQuestion);
        // Require >= 2 tokens before judging a topic shift; single-token follow-ups
        // ("왜?", "예를 들면?") have too little signal. Note: "왜" is 1 char → filtered by
        // tokenize()'s length >= 2 rule, so "왜?" yields 0 tokens → size < 2 → false.
        if (currentTokens.size() < 2) {
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

    private String enrichAnswer(
            String answer,
            boolean isClarification,
            List<String> clarificationOptions,
            boolean weakGrounding,
            List<String> suggestedNextQuestions
    ) {
        if (isClarification) {
            return appendListIfMissing(answer, CLARIFICATION_HEADING, clarificationOptions);
        }
        if (weakGrounding) {
            return appendListIfMissing(answer, SUGGESTED_QUESTION_HEADING, suggestedNextQuestions);
        }
        return answer;
    }

    private String appendListIfMissing(String answer, String heading, List<String> items) {
        if (items == null || items.isEmpty()) {
            return answer;
        }
        if (items.stream().allMatch(answer::contains)) {
            return answer;
        }

        StringBuilder builder = new StringBuilder(answer.strip());
        if (!builder.isEmpty()) {
            builder.append("\n\n");
        }
        builder.append(heading);
        items.forEach(item -> builder.append("\n- ").append(item));
        return builder.toString();
    }

    private List<String> extractBulletList(String answer, String heading) {
        int headingIndex = answer.indexOf(heading);
        if (headingIndex < 0) {
            return List.of();
        }

        String remaining = answer.substring(headingIndex + heading.length());
        List<String> items = new ArrayList<>();
        boolean readingBullets = false;
        for (String line : remaining.split("\\R")) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                if (readingBullets) {
                    break;
                }
                continue;
            }
            if (trimmed.startsWith("-")) {
                items.add(trimmed.substring(1).strip());
                readingBullets = true;
                continue;
            }
            if (readingBullets) {
                break;
            }
        }
        return sanitizeList(items);
    }

    private void appendStreamChunk(
            ChatCompletionChunk chunk,
            Consumer<String> tokenConsumer,
            StringBuilder accumulated
    ) {
        for (ChatCompletionChunk.Choice choice : chunk.choices()) {
            choice.delta().content().ifPresent(token -> {
                tokenConsumer.accept(token);
                accumulated.append(token);
            });
        }
    }

    private ChatRequestContext prepareChatRequest(
            String sessionId, String projectExternalId, String userQuestion, User user) {
        WikiRetrievalContext context = retrievalService.retrieveContext(projectExternalId, userQuestion);
        boolean hadActiveSession = sessionStore.hasActiveSession(sessionId);
        List<ChatTurn> redisTurns = sessionStore.loadRecentTurns(sessionId, projectExternalId);
        // If Redis is cold but user is authenticated, load from DB (resumed session)
        List<ChatTurn> previousTurns = redisTurns;
        if (redisTurns.isEmpty() && user != null) {
            previousTurns = persistenceService.loadRecentMessages(sessionId, MAX_PROMPT_TURNS);
        }
        boolean sessionReset = hadActiveSession && redisTurns.isEmpty();
        boolean topicShift = isTopicShift(previousTurns, userQuestion);
        List<ChatTurn> promptTurns = topicShift ? List.of() : selectPromptTurns(previousTurns);
        int clarificationTurns = countClarificationTurns(previousTurns);
        return new ChatRequestContext(context, promptTurns, topicShift, sessionReset, clarificationTurns);
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

    private record ChatRequestContext(
            WikiRetrievalContext context,
            List<ChatTurn> promptTurns,
            boolean topicShift,
            boolean sessionReset,
            int clarificationTurns
    ) {
    }
}
