package kr.devport.api.domain.wiki.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.core.http.StreamResponse;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.ProjectRepository;
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalChatResult;
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalChatResult.RelatedProjectLlmOutput;
import kr.devport.api.domain.wiki.dto.internal.WikiGlobalRetrievalContext;
import kr.devport.api.domain.wiki.dto.response.RelatedProjectResponse;
import kr.devport.api.domain.wiki.dto.response.WikiGlobalChatResponse;
import kr.devport.api.domain.wiki.enums.WikiChatSessionType;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore;
import kr.devport.api.domain.wiki.store.WikiChatSessionStore.ChatTurn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Global wiki chat service — discovers relevant projects across all wikis.
 * Returns structured JSON with answer + related project list.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiGlobalChatService {

    private static final int MAX_PROMPT_TURNS = 10;

    private final WikiGlobalRetrievalService retrievalService;
    private final WikiChatSessionStore sessionStore;
    private final WikiChatSessionPersistenceService persistenceService;
    private final WikiChatTitleService titleService;
    private final ProjectRepository projectRepository;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WikiGlobalChatResponse chatResult(String sessionId, String question, User user) {
        WikiGlobalRetrievalContext context = retrievalService.retrieve(question);
        List<ChatTurn> promptTurns = loadContextTurns(sessionId, user);

        List<ChatCompletionMessageParam> messages = buildMessages(context, promptTurns, question);

        ChatCompletion completion = openAIClient.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_4O_MINI)
                        .messages(messages)
                        .responseFormat(buildResponseFormat())
                        .build()
        );

        String payload = completion.choices().getFirst().message().content().orElse("");
        WikiGlobalChatResult result = parseResult(payload);

        // Persist
        boolean isFirst = persistenceService.isFirstMessage(sessionId);
        if (user != null) {
            var session = persistenceService.findOrCreateSession(sessionId, user, null, WikiChatSessionType.GLOBAL);
            persistenceService.saveUserMessage(session, question);
            persistenceService.saveAssistantMessage(session, result.answer(), false);
            if (isFirst) {
                titleService.generateAndSave(sessionId, question);
            }
        }
        sessionStore.saveTurn(sessionId, null, question, result.answer(), false);

        List<RelatedProjectResponse> enriched = enrichProjects(result.relatedProjects());

        return new WikiGlobalChatResponse(result.answer(), enriched, result.hasRelatedProjects(), sessionId);
    }

    public WikiGlobalChatResponse streamChatResult(
            String sessionId,
            String question,
            Consumer<String> tokenConsumer,
            User user
    ) {
        WikiGlobalRetrievalContext context = retrievalService.retrieve(question);
        List<ChatTurn> promptTurns = loadContextTurns(sessionId, user);

        // Streaming for global chat: stream plain text answer, then return full result
        // We use non-streaming with JSON for reliability; stream the answer portion only
        List<ChatCompletionMessageParam> messages = buildStreamMessages(context, promptTurns, question);

        StringBuilder accumulated = new StringBuilder();

        try (StreamResponse<ChatCompletionChunk> completionStream = openAIClient.chat().completions().createStreaming(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_4O_MINI)
                        .messages(messages)
                        .build()
        );
             java.util.stream.Stream<ChatCompletionChunk> chunks = completionStream.stream()) {
            chunks.forEach(chunk -> {
                for (ChatCompletionChunk.Choice choice : chunk.choices()) {
                    choice.delta().content().ifPresent(token -> {
                        tokenConsumer.accept(token);
                        accumulated.append(token);
                    });
                }
            });
        }

        String answer = accumulated.toString().strip();

        // Persist after stream
        boolean isFirst = persistenceService.isFirstMessage(sessionId);
        if (user != null) {
            var session = persistenceService.findOrCreateSession(sessionId, user, null, WikiChatSessionType.GLOBAL);
            persistenceService.saveUserMessage(session, question);
            persistenceService.saveAssistantMessage(session, answer, false);
            if (isFirst) {
                titleService.generateAndSave(sessionId, question);
            }
        }
        sessionStore.saveTurn(sessionId, null, question, answer, false);

        // For streaming, related projects come from the retrieval context (no JSON parsing)
        List<RelatedProjectLlmOutput> relatedFromContext = context.scoredProjects().stream()
                .map(sp -> new RelatedProjectLlmOutput(sp.projectExternalId(), ""))
                .toList();

        List<RelatedProjectResponse> enriched = enrichProjects(relatedFromContext);

        return new WikiGlobalChatResponse(answer, enriched, !enriched.isEmpty(), sessionId);
    }

    private List<ChatTurn> loadContextTurns(String sessionId, User user) {
        List<ChatTurn> redisTurns = sessionStore.loadRecentTurns(sessionId, null);
        if (!redisTurns.isEmpty()) {
            return redisTurns;
        }
        if (user != null) {
            List<ChatTurn> dbTurns = persistenceService.loadRecentMessages(sessionId, MAX_PROMPT_TURNS);
            if (!dbTurns.isEmpty()) {
                return dbTurns;
            }
        }
        return List.of();
    }

    private List<ChatCompletionMessageParam> buildMessages(
            WikiGlobalRetrievalContext context,
            List<ChatTurn> previousTurns,
            String question
    ) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(buildSystemPrompt(context))
                        .build()
        ));
        for (ChatTurn turn : previousTurns) {
            messages.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(turn.getQuestion()).build()
            ));
            messages.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(turn.getAnswer()).build()
            ));
        }
        messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content("질문: " + question + "\n\nJSON으로만 응답하세요.")
                        .build()
        ));
        return messages;
    }

    private List<ChatCompletionMessageParam> buildStreamMessages(
            WikiGlobalRetrievalContext context,
            List<ChatTurn> previousTurns,
            String question
    ) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content(buildStreamSystemPrompt(context))
                        .build()
        ));
        for (ChatTurn turn : previousTurns) {
            messages.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(turn.getQuestion()).build()
            ));
            messages.add(ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(turn.getAnswer()).build()
            ));
        }
        messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content("질문: " + question)
                        .build()
        ));
        return messages;
    }

    private String buildSystemPrompt(WikiGlobalRetrievalContext context) {
        return """
                You are a developer tool that helps users find relevant open-source projects.
                Given context from multiple project wikis, identify which projects best match the user's query.
                Always write the answer in Korean only.

                Return JSON with this exact schema:
                {
                  "answer": "1-2 sentence conversational response in Korean",
                  "relatedProjects": [
                    { "projectExternalId": "...", "relevanceReason": "Why this project matches (1 sentence in Korean)" }
                  ],
                  "hasRelatedProjects": true/false
                }

                If no projects are truly relevant, return an empty relatedProjects array and set hasRelatedProjects to false.
                Be honest — do not invent relevance.

                Multi-Project Context:
                %s
                """.formatted(context.groundedContext());
    }

    private String buildStreamSystemPrompt(WikiGlobalRetrievalContext context) {
        return """
                You are a developer tool that helps users find relevant open-source projects.
                Given context from multiple project wikis, answer the user's question concisely in Korean.
                Be conversational and helpful. Focus on which projects are most relevant and why.

                Multi-Project Context:
                %s
                """.formatted(context.groundedContext());
    }

    private WikiGlobalChatResult parseResult(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String answer = root.path("answer").asText("").strip();
            boolean hasRelated = root.path("hasRelatedProjects").asBoolean(false);
            List<RelatedProjectLlmOutput> relatedProjects = new ArrayList<>();
            JsonNode arr = root.path("relatedProjects");
            if (arr.isArray()) {
                arr.forEach(node -> {
                    String projectId = node.path("projectExternalId").asText("").strip();
                    String reason = node.path("relevanceReason").asText("").strip();
                    if (!projectId.isBlank()) {
                        relatedProjects.add(new RelatedProjectLlmOutput(projectId, reason));
                    }
                });
            }
            return new WikiGlobalChatResult(answer, relatedProjects, hasRelated, false);
        } catch (Exception e) {
            log.warn("wiki-global-chat: Failed to parse LLM response: {}", e.getMessage());
            return new WikiGlobalChatResult("죄송합니다, 응답을 처리하는 중 문제가 발생했습니다.", List.of(), false, false);
        }
    }

    private List<RelatedProjectResponse> enrichProjects(List<RelatedProjectLlmOutput> llmOutputs) {
        return llmOutputs.stream()
                .map(llmOutput -> {
                    try {
                        return projectRepository.findByExternalId(llmOutput.projectExternalId())
                                .map(project -> new RelatedProjectResponse(
                                        project.getExternalId(),
                                        project.getFullName(),
                                        project.getDescription(),
                                        llmOutput.relevanceReason(),
                                        project.getStars() != null ? project.getStars() : 0
                                ))
                                .orElse(null);
                    } catch (Exception e) {
                        log.warn("wiki-global-chat: Failed to enrich project {}: {}", llmOutput.projectExternalId(), e.getMessage());
                        return null;
                    }
                })
                .filter(r -> r != null)
                .toList();
    }

    private ResponseFormatJsonSchema buildResponseFormat() {
        var schema = ResponseFormatJsonSchema.JsonSchema.Schema.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "answer", Map.of("type", "string"),
                        "relatedProjects", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "projectExternalId", Map.of("type", "string"),
                                                "relevanceReason", Map.of("type", "string")
                                        ),
                                        "required", List.of("projectExternalId", "relevanceReason"),
                                        "additionalProperties", false
                                )
                        ),
                        "hasRelatedProjects", Map.of("type", "boolean")
                )))
                .putAdditionalProperty("required", JsonValue.from(List.of("answer", "relatedProjects", "hasRelatedProjects")))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build();

        return ResponseFormatJsonSchema.builder()
                .jsonSchema(ResponseFormatJsonSchema.JsonSchema.builder()
                        .name("wiki_global_chat_result")
                        .strict(true)
                        .schema(schema)
                        .build())
                .build();
    }
}
