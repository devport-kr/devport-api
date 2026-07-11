package kr.devport.api.domain.common.config

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenAIConfig(
    @param:Value("\${app.openai.api-key}") private val apiKey: String,
) {
    @Bean
    fun openAIClient(): OpenAIClient = OpenAIOkHttpClient.builder().apiKey(apiKey).build()
}
