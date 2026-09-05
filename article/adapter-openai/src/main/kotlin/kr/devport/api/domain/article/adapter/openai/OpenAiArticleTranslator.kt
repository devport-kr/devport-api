package kr.devport.api.domain.article.adapter.openai

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.openai.client.OpenAIClient
import com.openai.core.JsonValue
import com.openai.models.ChatModel
import com.openai.models.ReasoningEffort
import com.openai.models.ResponseFormatJsonSchema
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import kr.devport.api.domain.article.infrastructure.ArticleTranslator
import kr.devport.api.domain.article.infrastructure.LLMArticleResult
import kr.devport.api.domain.common.exception.LLMProcessingException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * OpenAI adapter for [ArticleTranslator]: translates an English article into a comprehensive Korean
 * rendering via the Chat Completions API with a strict JSON schema.
 */
@Service
class OpenAiArticleTranslator(
    private val openAIClient: OpenAIClient,
    @param:Value("\${app.openai.model}") private val model: String,
    @param:Value("\${app.openai.max-completion-tokens}") maxCompletionTokens: Int,
) : ArticleTranslator {
    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
    private val maxCompletionTokens: Int = minOf(maxCompletionTokens, 128000)

    override fun processArticle(
        titleEn: String?,
        url: String?,
        content: String?,
        tags: List<String>?,
    ): LLMArticleResult {
        val prompt = buildPrompt(titleEn, url, content, tags)

        try {
            val messages =
                listOf(
                    ChatCompletionMessageParam.ofSystem(
                        ChatCompletionSystemMessageParam.builder().content(SYSTEM_MESSAGE).build(),
                    ),
                    ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder().content(prompt).build(),
                    ),
                )

            val params =
                ChatCompletionCreateParams
                    .builder()
                    .model(ChatModel.of(model))
                    .messages(messages)
                    .maxCompletionTokens(maxCompletionTokens.toLong())
                    .reasoningEffort(ReasoningEffort.LOW)
                    .responseFormat(buildResponseFormat())
                    .build()

            val completion = openAIClient.chat().completions().create(params)

            val responseContent = completion.choices().first().message().content().orElse("")
            log.info("OpenAI response: finish_reason={}, model={}", completion.choices().first().finishReason(), model)

            return parseResponse(responseContent, url)
        } catch (e: LLMProcessingException) {
            throw e
        } catch (e: Exception) {
            log.error("LLM processing failed for URL: {}", url, e)
            throw LLMProcessingException("LLM processing failed: ${e.message}", e)
        }
    }

    private fun buildPrompt(
        titleEn: String?,
        url: String?,
        content: String?,
        tags: List<String>?,
    ): String {
        val tagsStr =
            if (!tags.isNullOrEmpty()) {
                tags.subList(0, minOf(tags.size, 10)).joinToString(", ")
            } else {
                "(none)"
            }

        val contentStr = if (!content.isNullOrBlank()) content.trim() else ""

        val articlesText =
            StringBuilder()
                .append("--- Article 1 ---\n")
                .append("Title: ")
                .append(titleEn)
                .append('\n')
                .append("URL: ")
                .append(url)
                .append('\n')
                .append("Tags: ")
                .append(tagsStr)
                .append('\n')

        if (contentStr.isNotBlank()) {
            articlesText.append("Content:\n").append(contentStr).append('\n')
        } else {
            articlesText.append("Content: (not available)\n")
        }
        articlesText.append('\n')

        return PROMPT_TEMPLATE
            .replace("__DEVPORT_ARTICLE_COUNT__", "1")
            .replace("__DEVPORT_ARTICLES_TEXT__", articlesText.toString())
    }

    private fun buildResponseFormat(): ResponseFormatJsonSchema {
        val schema =
            ResponseFormatJsonSchema.JsonSchema.Schema
                .builder()
                .putAdditionalProperty("type", JsonValue.from("object"))
                .putAdditionalProperty(
                    "properties",
                    JsonValue.from(
                        mapOf(
                            "articles" to
                                mapOf(
                                    "type" to "array",
                                    "items" to
                                        mapOf(
                                            "type" to "object",
                                            "properties" to
                                                mapOf(
                                                    "url" to mapOf("type" to "string"),
                                                    "is_technical" to mapOf("type" to "boolean"),
                                                    "title_ko" to mapOf("type" to "string"),
                                                    "summary_ko" to mapOf("type" to "string"),
                                                    "category" to
                                                        mapOf(
                                                            "type" to "string",
                                                            "enum" to
                                                                listOf(
                                                                    "AI_LLM", "DEVOPS_SRE", "INFRA_CLOUD", "DATABASE",
                                                                    "BLOCKCHAIN", "SECURITY", "DATA_SCIENCE", "ARCHITECTURE",
                                                                    "MOBILE", "FRONTEND", "BACKEND", "OTHER",
                                                                ),
                                                        ),
                                                    "tags" to mapOf("type" to "array", "items" to mapOf("type" to "string")),
                                                ),
                                            "required" to listOf("url", "is_technical", "title_ko", "summary_ko", "category", "tags"),
                                            "additionalProperties" to false,
                                        ),
                                ),
                        ),
                    ),
                ).putAdditionalProperty("required", JsonValue.from(listOf("articles")))
                .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                .build()

        return ResponseFormatJsonSchema
            .builder()
            .jsonSchema(
                ResponseFormatJsonSchema.JsonSchema
                    .builder()
                    .name("article_summaries")
                    .strict(true)
                    .schema(schema)
                    .build(),
            ).build()
    }

    private fun parseResponse(
        content: String,
        originalUrl: String?,
    ): LLMArticleResult {
        try {
            val root = objectMapper.readTree(content)

            // Handle structured output wrapper: {"articles": [...]}
            val articlesNode = if (root.has("articles")) root.get("articles") else root

            val article: JsonNode =
                when {
                    articlesNode.isArray && !articlesNode.isEmpty -> articlesNode.get(0)
                    articlesNode.isObject -> articlesNode
                    else -> throw LLMProcessingException("Unexpected LLM response format")
                }

            val isTechnical = article.path("is_technical").asBoolean(false)
            var titleKo = article.path("title_ko").asText("")
            val summaryKo = article.path("summary_ko").asText("")
            val category = article.path("category").asText("OTHER")
            val url = article.path("url").asText(originalUrl)

            val tags = mutableListOf<String>()
            val tagsNode = article.get("tags")
            if (tagsNode != null && tagsNode.isArray) {
                for (tag in tagsNode) {
                    val t = tag.asText("").trim().lowercase().replace(" ", "-")
                    if (t.isNotEmpty() && tags.size < 5) {
                        tags.add(t)
                    }
                }
            }

            if (titleKo.length > 100) {
                titleKo = titleKo.substring(0, 100)
            }

            return LLMArticleResult(isTechnical, titleKo, summaryKo, category, tags, url)
        } catch (e: JsonProcessingException) {
            log.error("Failed to parse LLM response as JSON for url={}", originalUrl, e)
            throw LLMProcessingException("Failed to parse LLM response", e)
        }
    }

    companion object {
        private val SYSTEM_MESSAGE =
            """
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
            - Only trim genuinely redundant phrasing — never skip entire paragraphs or sections.
            """.trimIndent()

        private val PROMPT_TEMPLATE =
            """
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
                "summary_ko": "## 개요\n\n이 글은 Python의 asyncio 라이브러리를 활용한 비동기 처리 방법을 깊이 있게 다룹니다. 동시성(concurrency)과 병렬성(parallelism)의 차이를 명확히 구분하고, 실제 프로덕션 환경에서 async/await 패턴을 효과적으로 사용하는 방법을 설명합니다.\n\n## async/await 패턴의 기본 사용법\n\nPython 3.5에서 도입된 `async/await` 구문은 비동기 코드를 동기 코드처럼 읽기 쉽게 작성할 수 있게 해줍니다. 기본적인 패턴은 다음과 같습니다:\n\n```python\nasync def fetch_data(url):\n    async with aiohttp.ClientSession() as session:\n        async with session.get(url) as response:\n            return await response.json()\n\nasync def main():\n    results = await asyncio.gather(\n        fetch_data('https://api.example.com/users'),\n        fetch_data('https://api.example.com/posts')\n    )\n```\n\n`asyncio.gather()`를 사용하면 여러 코루틴을 동시에 실행하여 I/O 바운드 작업에서 상당한 성능 향상을 얻을 수 있습니다. 저자는 실제 프로젝트에서 API 호출 시간을 60% 이상 단축한 사례를 공유합니다.\n\n## 동시성 vs 병렬성\n\n동시성은 여러 작업을 번갈아 처리하는 것이고, 병렬성은 여러 작업을 실제로 동시에 처리하는 것입니다. asyncio는 동시성을 제공하며, 이는 네트워크 요청이나 파일 I/O처럼 대기 시간이 긴 작업에 특히 효과적입니다.\n\n## 실전 팁과 주의사항\n\n저자는 CPU 바운드 작업에서는 asyncio 대신 `multiprocessing`을 사용할 것을 권장하며, 혼합 워크로드에서는 `loop.run_in_executor()`를 활용한 하이브리드 접근법을 제안합니다. 또한 에러 처리, 타임아웃 설정, 디버깅 기법 등 프로덕션 환경에서 겪는 현실적인 문제와 해결책을 상세히 다룹니다.",
                "category": "BACKEND",
                "tags": ["python", "async", "concurrency"]
              }
            ]
            ```

            JSON rules:
            - Newlines inside summary_ko must be \n (escaped)
            - Quotes inside strings must be \" (escaped)
            - Return ONLY valid JSON — no trailing commas, no comments
            """.trimIndent()
    }
}
