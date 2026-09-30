package kr.devport.api.domain.article.service.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.ReasoningEffort;
import com.openai.models.ResponseFormatJsonSchema;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import kr.devport.api.domain.common.exception.LLMProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ArticleLLMService {

    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxCompletionTokens;

    private static final String SYSTEM_MESSAGE = """
        You are an expert English-to-Korean technical translator and editor specializing in software engineering content.

        ## Your Task
        You produce **comprehensive Korean translations** of English tech articles. This is a TRANSLATION that is slightly condensed — NOT a summary or abstract. The Korean output should be approximately 70-80% of the original article's length. A Korean reader should fully understand the article without needing to read the original.

        ## Korean Writing Rules
        - Write natural, fluent Korean as if the author originally wrote in Korean. Avoid literal translation patterns (e.g., '~하는 것이다', '~되어진다', '~할 수 있습니다' repetition).
        - Use industry-standard Korean terms (e.g., 'deployment'→'배포', 'scalability'→'확장성').
        - Keep proper nouns in English (React, Kubernetes, AWS, PostgreSQL, Kafka, etc.).
        - Preserve the original author's tone and voice: formal→formal, casual→casual, opinionated→opinionated.
        - Do NOT translate code blocks — include them exactly as-is.
        - Use markdown formatting (##, ###, -, **, `) to mirror the original structure.

        ## Critical: Length and Detail
        - LONGER output is ALWAYS better than missing content. Never cut for brevity.
        - Include EVERY key argument, technical detail, example, and insight from the original.
        - Preserve the article's section structure, heading hierarchy, and logical flow.
        - Only trim genuinely redundant phrasing — never skip entire paragraphs or sections.""";

    public ArticleLLMService(
        OpenAIClient openAIClient,
        @Value("${app.openai.model}") String model,
        @Value("${app.openai.max-completion-tokens}") int maxCompletionTokens
    ) {
        this.openAIClient = openAIClient;
        this.objectMapper = new ObjectMapper();
        this.model = model;
        this.maxCompletionTokens = Math.min(maxCompletionTokens, 128000);
    }

    public record LLMArticleResult(
        boolean isTechnical,
        String titleKo,
        String summaryKo,
        String category,
        List<String> tags,
        String url
    ) {}

    public LLMArticleResult processArticle(String titleEn, String url, String content, List<String> tags) {
        String prompt = buildPrompt(titleEn, url, content, tags);

        try {
            List<ChatCompletionMessageParam> messages = List.of(
                ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder()
                        .content(SYSTEM_MESSAGE)
                        .build()
                ),
                ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder()
                        .content(prompt)
                        .build()
                )
            );

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ChatModel.of(model))
                .messages(messages)
                .maxCompletionTokens(maxCompletionTokens)
                .reasoningEffort(ReasoningEffort.LOW)
                .responseFormat(buildResponseFormat())
                .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);

            String responseContent = completion.choices().getFirst().message().content().orElse("");
            log.info("OpenAI response: finish_reason={}, model={}",
                completion.choices().getFirst().finishReason(), model);

            return parseResponse(responseContent, url);
        } catch (LLMProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM processing failed for URL: {}", url, e);
            throw new LLMProcessingException("LLM processing failed: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(String titleEn, String url, String content, List<String> tags) {
        String tagsStr = "(none)";
        if (tags != null && !tags.isEmpty()) {
            tagsStr = String.join(", ", tags.subList(0, Math.min(tags.size(), 10)));
        }

        String contentStr = (content != null && !content.isBlank()) ? content.strip() : "";

        StringBuilder articlesText = new StringBuilder()
            .append("--- Article 1 ---\n")
            .append("Title: ").append(titleEn).append('\n')
            .append("URL: ").append(url).append('\n')
            .append("Tags: ").append(tagsStr).append('\n');

        if (!contentStr.isBlank()) {
            articlesText.append("Content:\n").append(contentStr).append('\n');
        } else {
            articlesText.append("Content: (not available)\n");
        }
        articlesText.append('\n');

        String promptTemplate = """
            Translate the following __DEVPORT_ARTICLE_COUNT__ English developer article(s) into comprehensive Korean.

            ## Articles

            __DEVPORT_ARTICLES_TEXT__

            ## Instructions

            For each article, produce a JSON object with these 6 fields:

            ### 1. is_technical (boolean)
            Is this article useful or interesting for software developers?
            - TRUE: tutorials, code, architecture, dev tools, frameworks, system design, security, infra, AI/ML, developer career, tech startup products
            - FALSE: pure politics, non-tech business, consumer product reviews, social issues unrelated to tech
            - Ask yourself: "Would a developer building software care about this?"

            ### 2. title_ko (string, max 100 characters)
            A concise Korean title capturing the core topic.

            ### 3. summary_ko (string, markdown format)
            Write a **comprehensive Korean translation** of the full article. This is a faithful translation that is slightly condensed — NOT a summary, NOT an abstract, NOT a brief overview.

            **Length target: aim for 70-80% of the original article's length.** Longer output is always better than missing content.

            Requirements:
            - Translate EVERY section of the article — do NOT skip or merge sections
            - Preserve the original section structure, heading hierarchy (##, ###), and logical flow
            - Include ALL key arguments, explanations, technical details, examples, and insights
            - Include code examples from the original exactly as-is (do not translate code)
            - Follow the author's original flow and order — do NOT reorder or restructure
            - Preserve the author's tone and voice (if opinionated, keep the opinion; if humorous, keep the humor)
            - A Korean reader must be able to fully understand the article without reading the English original
            - Only trim genuinely redundant or repetitive phrasing — NEVER skip entire paragraphs or ideas
            - If content is unavailable (title only), write a brief description based on the title. Do NOT fabricate details.

            ### 4. category (string)
            Pick the single best match: AI_LLM, DEVOPS_SRE, INFRA_CLOUD, DATABASE, BLOCKCHAIN, SECURITY, DATA_SCIENCE, ARCHITECTURE, MOBILE, FRONTEND, BACKEND, OTHER

            ### 5. tags (array of strings)
            3-5 lowercase tags. Use hyphens instead of spaces.

            ### 6. url (string)
            Return the input URL exactly as given (used for matching).

            ## Output Format

            Return a JSON array inside a ```json code fence. Maintain the same article order.
            No text outside the code fence.

            ```json
            [
              {
                "url": "https://example.com/article1",
                "is_technical": true,
                "title_ko": "Python에서 비동기 처리 완벽 가이드",
                "summary_ko": "## 개요\\n\\n이 글은 Python의 asyncio 라이브러리를 활용한 비동기 처리 방법을 깊이 있게 다룹니다. 동시성(concurrency)과 병렬성(parallelism)의 차이를 명확히 구분하고, 실제 프로덕션 환경에서 async/await 패턴을 효과적으로 사용하는 방법을 설명합니다.\\n\\n## async/await 패턴의 기본 사용법\\n\\nPython 3.5에서 도입된 `async/await` 구문은 비동기 코드를 동기 코드처럼 읽기 쉽게 작성할 수 있게 해줍니다. 기본적인 패턴은 다음과 같습니다:\\n\\n```python\\nasync def fetch_data(url):\\n    async with aiohttp.ClientSession() as session:\\n        async with session.get(url) as response:\\n            return await response.json()\\n\\nasync def main():\\n    results = await asyncio.gather(\\n        fetch_data('https://api.example.com/users'),\\n        fetch_data('https://api.example.com/posts')\\n    )\\n```\\n\\n`asyncio.gather()`를 사용하면 여러 코루틴을 동시에 실행하여 I/O 바운드 작업에서 상당한 성능 향상을 얻을 수 있습니다. 저자는 실제 프로젝트에서 API 호출 시간을 60% 이상 단축한 사례를 공유합니다.\\n\\n## 동시성 vs 병렬성\\n\\n동시성은 여러 작업을 번갈아 처리하는 것이고, 병렬성은 여러 작업을 실제로 동시에 처리하는 것입니다. asyncio는 동시성을 제공하며, 이는 네트워크 요청이나 파일 I/O처럼 대기 시간이 긴 작업에 특히 효과적입니다.\\n\\n## 실전 팁과 주의사항\\n\\n저자는 CPU 바운드 작업에서는 asyncio 대신 `multiprocessing`을 사용할 것을 권장하며, 혼합 워크로드에서는 `loop.run_in_executor()`를 활용한 하이브리드 접근법을 제안합니다. 또한 에러 처리, 타임아웃 설정, 디버깅 기법 등 프로덕션 환경에서 겪는 현실적인 문제와 해결책을 상세히 다룹니다.",
                "category": "BACKEND",
                "tags": ["python", "async", "concurrency"]
              }
            ]
            ```

            JSON rules:
            - Newlines inside summary_ko must be \\n (escaped)
            - Quotes inside strings must be \\" (escaped)
            - Return ONLY valid JSON — no trailing commas, no comments""";

        return promptTemplate
            .replace("__DEVPORT_ARTICLE_COUNT__", "1")
            .replace("__DEVPORT_ARTICLES_TEXT__", articlesText.toString());
    }

    private ResponseFormatJsonSchema buildResponseFormat() {
        var schema = ResponseFormatJsonSchema.JsonSchema.Schema.builder()
            .putAdditionalProperty("type", JsonValue.from("object"))
            .putAdditionalProperty("properties", JsonValue.from(java.util.Map.of(
                "articles", java.util.Map.of(
                    "type", "array",
                    "items", java.util.Map.of(
                        "type", "object",
                        "properties", java.util.Map.of(
                            "url", java.util.Map.of("type", "string"),
                            "is_technical", java.util.Map.of("type", "boolean"),
                            "title_ko", java.util.Map.of("type", "string"),
                            "summary_ko", java.util.Map.of("type", "string"),
                            "category", java.util.Map.of("type", "string", "enum", List.of(
                                "AI_LLM", "DEVOPS_SRE", "INFRA_CLOUD", "DATABASE",
                                "BLOCKCHAIN", "SECURITY", "DATA_SCIENCE", "ARCHITECTURE",
                                "MOBILE", "FRONTEND", "BACKEND", "OTHER"
                            )),
                            "tags", java.util.Map.of("type", "array", "items", java.util.Map.of("type", "string"))
                        ),
                        "required", List.of("url", "is_technical", "title_ko", "summary_ko", "category", "tags"),
                        "additionalProperties", false
                    )
                )
            )))
            .putAdditionalProperty("required", JsonValue.from(List.of("articles")))
            .putAdditionalProperty("additionalProperties", JsonValue.from(false))
            .build();

        return ResponseFormatJsonSchema.builder()
            .jsonSchema(
                ResponseFormatJsonSchema.JsonSchema.builder()
                    .name("article_summaries")
                    .strict(true)
                    .schema(schema)
                    .build()
            )
            .build();
    }

    private LLMArticleResult parseResponse(String content, String originalUrl) {
        try {
            JsonNode root = objectMapper.readTree(content);

            // Handle structured output wrapper: {"articles": [...]}
            JsonNode articlesNode = root.has("articles") ? root.get("articles") : root;

            JsonNode article;
            if (articlesNode.isArray() && !articlesNode.isEmpty()) {
                article = articlesNode.get(0);
            } else if (articlesNode.isObject()) {
                article = articlesNode;
            } else {
                throw new LLMProcessingException("Unexpected LLM response format");
            }

            boolean isTechnical = article.path("is_technical").asBoolean(false);
            String titleKo = article.path("title_ko").asText("");
            String summaryKo = article.path("summary_ko").asText("");
            String category = article.path("category").asText("OTHER");
            String url = article.path("url").asText(originalUrl);

            List<String> tags = new ArrayList<>();
            JsonNode tagsNode = article.get("tags");
            if (tagsNode != null && tagsNode.isArray()) {
                for (JsonNode tag : tagsNode) {
                    String t = tag.asText("").strip().toLowerCase().replace(" ", "-");
                    if (!t.isEmpty() && tags.size() < 5) {
                        tags.add(t);
                    }
                }
            }

            if (titleKo.length() > 100) {
                titleKo = titleKo.substring(0, 100);
            }

            return new LLMArticleResult(isTechnical, titleKo, summaryKo, category, tags, url);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response as JSON for url={}", originalUrl, e);
            throw new LLMProcessingException("Failed to parse LLM response", e);
        }
    }
}
