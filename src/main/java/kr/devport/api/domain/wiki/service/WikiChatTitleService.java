package kr.devport.api.domain.wiki.service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import kr.devport.api.domain.wiki.repository.WikiChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Async LLM title generation for wiki chat sessions.
 * Generates a concise 5-7 word title from the first question.
 * Fails silently — never breaks user's chat.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WikiChatTitleService {

    private final WikiChatSessionRepository sessionRepository;
    private final OpenAIClient openAIClient;

    @Async
    @Transactional
    public void generateAndSave(String sessionExternalId, String firstQuestion) {
        try {
            String prompt = "다음 첫 질문으로 시작하는 대화의 제목을 5~7단어로 간결하게 한국어로 작성하세요: '"
                    + firstQuestion + "'. 제목만 출력하고 따옴표나 마침표는 붙이지 마세요.";

            List<ChatCompletionMessageParam> messages = List.of(
                    ChatCompletionMessageParam.ofUser(
                            ChatCompletionUserMessageParam.builder()
                                    .content(prompt)
                                    .build()
                    )
            );

            var completion = openAIClient.chat().completions().create(
                    ChatCompletionCreateParams.builder()
                            .model(ChatModel.GPT_4O_MINI)
                            .messages(messages)
                            .maxCompletionTokens(30L)
                            .build()
            );

            String title = completion.choices().getFirst().message().content().orElse("").strip();
            if (!title.isBlank()) {
                sessionRepository.findByExternalId(sessionExternalId).ifPresent(session -> {
                    session.setTitle(title);
                    sessionRepository.save(session);
                });
            }
        } catch (Exception e) {
            log.error("wiki-title: Failed to generate title for session={}: {}", sessionExternalId, e.getMessage());
        }
    }
}
