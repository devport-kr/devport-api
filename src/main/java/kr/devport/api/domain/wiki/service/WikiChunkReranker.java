package kr.devport.api.domain.wiki.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.models.ChatModel;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import kr.devport.api.domain.wiki.entity.WikiSectionChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WikiChunkReranker {

    private static final int MAX_CANDIDATES = 20;
    private static final int MAX_CONTENT_CHARS = 600;

    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ScoredChunk> rerank(String question, List<WikiSectionChunk> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<WikiSectionChunk> cappedCandidates = candidates.stream()
                .limit(MAX_CANDIDATES)
                .toList();

        ChatCompletion completion = openAIClient.chat().completions().create(
                ChatCompletionCreateParams.builder()
                        .model(ChatModel.GPT_4O_MINI)
                        .messages(buildMessages(question, cappedCandidates))
                        .responseFormat(buildResponseFormat())
                        .build()
        );

        String payload = completion.choices().getFirst().message().content().orElse("");
        List<ScoredChunk> parsed = parse(payload);
        if (parsed.isEmpty()) {
            return fallbackScores(cappedCandidates.size());
        }
        return parsed;
    }

    private List<ChatCompletionMessageParam> buildMessages(String question, List<WikiSectionChunk> candidates) {
        List<ChatCompletionMessageParam> messages = new ArrayList<>();
        messages.add(ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                        .content("""
                                You are reranking repository wiki chunks for retrieval.
                                Return JSON only.
                                Score chunks by how directly they help answer the question.
                                Higher score means more useful grounding.
                                Prefer chunks with exact technical relevance over broad summaries.
                                """)
                        .build()
        ));
        messages.add(ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                        .content(buildUserPrompt(question, candidates))
                        .build()
        ));
        return messages;
    }

    private String buildUserPrompt(String question, List<WikiSectionChunk> candidates) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Question:\n").append(question).append("\n\nCandidates:\n");
        for (int i = 0; i < candidates.size(); i++) {
            WikiSectionChunk chunk = candidates.get(i);
            prompt.append("Index: ").append(i).append("\n");
            prompt.append("Heading: ").append(resolveHeading(chunk)).append("\n");
            prompt.append("Content:\n").append(truncate(chunk.getContent())).append("\n\n");
        }
        prompt.append("""
                Return all candidates in a `scores` array.
                Each item must include:
                - index
                - score
                """);
        return prompt.toString();
    }

    private ResponseFormatJsonSchema buildResponseFormat() {
        var schema = ResponseFormatJsonSchema.JsonSchema.Schema.builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty("properties", JsonValue.from(Map.of(
                        "scores", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "index", Map.of("type", "integer"),
                                                "score", Map.of("type", "number")
                                        ),
                                        "required", List.of("index", "score"),
                                        "additionalProperties", false
                                )
                        )
                )))
                .putAdditionalProperty("required", JsonValue.from(List.of("scores")))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build();

        return ResponseFormatJsonSchema.builder()
                .jsonSchema(ResponseFormatJsonSchema.JsonSchema.builder()
                        .name("wiki_chunk_rerank_result")
                        .strict(true)
                        .schema(schema)
                        .build())
                .build();
    }

    private List<ScoredChunk> parse(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode scores = root.path("scores");
            if (!scores.isArray()) {
                return List.of();
            }
            List<ScoredChunk> parsed = new ArrayList<>();
            scores.forEach(node -> {
                if (node.has("index") && node.has("score")) {
                    parsed.add(new ScoredChunk(
                            node.path("index").asInt(-1),
                            node.path("score").asDouble(0.0d)
                    ));
                }
            });
            return parsed.stream()
                    .filter(score -> score.index() >= 0)
                    .sorted(java.util.Comparator.comparingDouble(ScoredChunk::score).reversed())
                    .toList();
        } catch (Exception e) {
            log.warn("wiki-reranker: failed to parse reranker output: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ScoredChunk> fallbackScores(int size) {
        List<ScoredChunk> fallback = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            fallback.add(new ScoredChunk(i, size - i));
        }
        return fallback;
    }

    private String truncate(String content) {
        return content.length() <= MAX_CONTENT_CHARS ? content : content.substring(0, MAX_CONTENT_CHARS) + "...";
    }

    private String resolveHeading(WikiSectionChunk chunk) {
        Map<String, Object> metadata = chunk.getMetadata();
        if (metadata != null) {
            Object titleKo = metadata.get("titleKo");
            if (titleKo != null && !String.valueOf(titleKo).isBlank()) {
                return String.valueOf(titleKo);
            }
        }
        if (chunk.getSubsectionId() != null && !chunk.getSubsectionId().isBlank()) {
            return chunk.getSectionId() + " > " + chunk.getSubsectionId();
        }
        return chunk.getSectionId();
    }

    public record ScoredChunk(int index, double score) {
    }
}
