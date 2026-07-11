package kr.devport.api.domain.wiki.adapter.openai

import com.openai.client.OpenAIClient
import com.openai.models.embeddings.EmbeddingCreateParams
import kr.devport.api.domain.wiki.infrastructure.EmbeddingPort
import org.springframework.stereotype.Component

/** OpenAI adapter for [EmbeddingPort] using the text-embedding-3-small model. */
@Component
class OpenAiEmbeddingAdapter(
    private val openAIClient: OpenAIClient,
) : EmbeddingPort {
    override fun embed(text: String): FloatArray {
        val params =
            EmbeddingCreateParams
                .builder()
                .model(EMBEDDING_MODEL)
                .input(text)
                .build()
        val response = openAIClient.embeddings().create(params)
        val embeddingFloats = response.data().first().embedding()
        return FloatArray(embeddingFloats.size) { embeddingFloats[it] }
    }

    companion object {
        private const val EMBEDDING_MODEL = "text-embedding-3-small"
    }
}
