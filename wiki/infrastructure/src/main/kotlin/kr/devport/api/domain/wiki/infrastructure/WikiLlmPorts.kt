package kr.devport.api.domain.wiki.infrastructure

/** A single chat message in a prompt. Pure domain shape — no provider types. */
data class ChatMessage(
    val role: ChatRole,
    val content: String,
)

enum class ChatRole { SYSTEM, USER, ASSISTANT }

/** Structured-output JSON schema spec. [schema] is a plain JSON-Schema map (object at the root). */
data class JsonSchemaSpec(
    val name: String,
    val schema: Map<String, Any>,
)

/**
 * Out-port: LLM chat completions. The OpenAI adapter in :wiki:adapter-openai implements it; the core
 * only sees this contract (no com.openai types leak into the use-cases).
 */
interface ChatPort {
    /** Non-streaming completion. Returns choices[0].message.content (empty string when absent). */
    fun complete(
        model: String,
        messages: List<ChatMessage>,
        maxCompletionTokens: Long? = null,
        jsonSchema: JsonSchemaSpec? = null,
    ): String

    /** Streaming completion. [onToken] is invoked for each content delta as it arrives. */
    fun stream(
        model: String,
        messages: List<ChatMessage>,
        onToken: (String) -> Unit,
    )
}

/**
 * Out-port: text embeddings (model "text-embedding-3-small"). Implemented by :wiki:adapter-openai.
 */
interface EmbeddingPort {
    fun embed(text: String): FloatArray
}
