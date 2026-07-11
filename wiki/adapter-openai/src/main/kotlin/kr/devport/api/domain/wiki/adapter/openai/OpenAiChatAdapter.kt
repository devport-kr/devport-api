package kr.devport.api.domain.wiki.adapter.openai

import com.openai.client.OpenAIClient
import com.openai.core.JsonValue
import com.openai.models.ChatModel
import com.openai.models.ResponseFormatJsonSchema
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import kr.devport.api.domain.wiki.infrastructure.ChatMessage
import kr.devport.api.domain.wiki.infrastructure.ChatPort
import kr.devport.api.domain.wiki.infrastructure.ChatRole
import kr.devport.api.domain.wiki.infrastructure.JsonSchemaSpec
import org.springframework.stereotype.Component

/**
 * OpenAI adapter for [ChatPort]: owns ChatMessage→provider-message mapping, JsonSchemaSpec→
 * ResponseFormatJsonSchema, max-token configuration, and the streaming delta loop. The wiki core
 * sees none of the com.openai types.
 */
@Component
class OpenAiChatAdapter(
    private val openAIClient: OpenAIClient,
) : ChatPort {
    override fun complete(
        model: String,
        messages: List<ChatMessage>,
        maxCompletionTokens: Long?,
        jsonSchema: JsonSchemaSpec?,
    ): String {
        val builder =
            ChatCompletionCreateParams
                .builder()
                .model(ChatModel.of(model))
                .messages(messages.map { it.toParam() })
        maxCompletionTokens?.let { builder.maxCompletionTokens(it) }
        jsonSchema?.let { builder.responseFormat(it.toResponseFormat()) }

        val completion = openAIClient.chat().completions().create(builder.build())
        return completion.choices().first().message().content().orElse("")
    }

    override fun stream(
        model: String,
        messages: List<ChatMessage>,
        onToken: (String) -> Unit,
    ) {
        val params =
            ChatCompletionCreateParams
                .builder()
                .model(ChatModel.of(model))
                .messages(messages.map { it.toParam() })
                .build()

        openAIClient
            .chat()
            .completions()
            .createStreaming(params)
            .use { completionStream ->
                completionStream.stream().use { chunks ->
                    chunks.forEach { chunk ->
                        for (choice in chunk.choices()) {
                            choice.delta().content().ifPresent { token -> onToken(token) }
                        }
                    }
                }
            }
    }

    private fun ChatMessage.toParam(): ChatCompletionMessageParam =
        when (role) {
            ChatRole.SYSTEM ->
                ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder().content(content).build(),
                )
            ChatRole.USER ->
                ChatCompletionMessageParam.ofUser(
                    ChatCompletionUserMessageParam.builder().content(content).build(),
                )
            ChatRole.ASSISTANT ->
                ChatCompletionMessageParam.ofAssistant(
                    ChatCompletionAssistantMessageParam.builder().content(content).build(),
                )
        }

    private fun JsonSchemaSpec.toResponseFormat(): ResponseFormatJsonSchema {
        val schemaBuilder = ResponseFormatJsonSchema.JsonSchema.Schema.builder()
        schema.forEach { (key, value) -> schemaBuilder.putAdditionalProperty(key, JsonValue.from(value)) }

        return ResponseFormatJsonSchema
            .builder()
            .jsonSchema(
                ResponseFormatJsonSchema.JsonSchema
                    .builder()
                    .name(name)
                    .strict(true)
                    .schema(schemaBuilder.build())
                    .build(),
            ).build()
    }
}
