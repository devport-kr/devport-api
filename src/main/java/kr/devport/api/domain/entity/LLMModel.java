package kr.devport.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "llm_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;  // e.g., "Claude Sonnet 4.5", "GPT-5", "Gemini 3 Pro"

    @Column(nullable = false, length = 100)
    private String provider;  // e.g., "Anthropic", "OpenAI", "Google"

    @Column(length = 50, name = "context_window")
    private String contextWindow;  // e.g., "200K", "10M" (optional)

    @Column(length = 50)
    private String pricing;  // e.g., "$3 / $15" (optional)

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
