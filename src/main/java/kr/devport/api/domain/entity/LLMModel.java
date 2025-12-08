package kr.devport.api.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // API identifiers
    /**
     * External ID from Artificial Analysis API (UUID format).
     * Example: "4559e9f0-8aad-4681-89fb-68cb915e0f16"
     */
    @Column(unique = true, length = 100, name = "external_id")
    private String externalId;

    /**
     * URL-friendly slug identifier from API.
     * Example: "qwen3-14b-instruct-reasoning"
     */
    @Column(unique = true, length = 200)
    private String slug;

    // Model identification
    @NotBlank
    @Column(nullable = false, unique = true, length = 100, name = "model_id")
    private String modelId;  // e.g., "gpt-4-turbo", "claude-opus-4-5"

    @NotBlank
    @Column(nullable = false, length = 200, name = "model_name")
    private String modelName;  // e.g., "GPT-4 Turbo", "Claude Opus 4.5"

    /**
     * Model release date from API.
     */
    @Column(name = "release_date")
    private LocalDate releaseDate;

    // Provider information (denormalized for easier queries)
    @Column(length = 100)
    private String provider;  // e.g., "OpenAI", "Anthropic", "Google" (can be null if using modelCreator)

    /**
     * Relationship to ModelCreator entity.
     * Contains detailed provider information from API.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_creator_id")
    private ModelCreator modelCreator;

    @Column(columnDefinition = "TEXT")
    private String description;  // Model description from API

    // Pricing (USD per million tokens)
    @DecimalMin("0.00")
    @Column(precision = 10, scale = 2, name = "price_input")
    private BigDecimal priceInput;  // Input token price

    @DecimalMin("0.00")
    @Column(precision = 10, scale = 2, name = "price_output")
    private BigDecimal priceOutput;  // Output token price

    @DecimalMin("0.00")
    @Column(precision = 10, scale = 2, name = "price_blended")
    private BigDecimal priceBlended;  // Blended price (3:1 ratio)

    // Performance metrics
    @Min(1)
    @Column(name = "context_window")
    private Long contextWindow;  // Max tokens (e.g., 1000000 for 1M)

    @DecimalMin("0.01")
    @Column(precision = 10, scale = 2, name = "output_speed_median")
    private BigDecimal outputSpeedMedian;  // Tokens per second (median)

    @DecimalMin("0.0001")
    @Column(precision = 10, scale = 4, name = "latency_ttft")
    private BigDecimal latencyTtft;  // Time to first token (seconds)

    @DecimalMin("0.0001")
    @Column(precision = 10, scale = 4, name = "median_time_to_first_answer_token")
    private BigDecimal medianTimeToFirstAnswerToken;  // Time to first answer token (seconds)

    @Column(length = 50)
    private String license;  // "Open" or "Proprietary"

    // Benchmark Scores (all 15 categories)
    // All scores are nullable (model may not be tested on all benchmarks)
    // All scores are 0-100 percentages

    // Agentic Capabilities (2)
    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_terminal_bench_hard")
    private BigDecimal scoreTerminalBenchHard;  // Agentic Coding & Terminal Use

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_tau_bench_telecom")
    private BigDecimal scoreTauBenchTelecom;  // Agentic Tool Use

    // Reasoning & Knowledge (4)
    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aa_lcr")
    private BigDecimal scoreAaLcr;  // Long Context Reasoning

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_humanitys_last_exam")
    private BigDecimal scoreHumanitysLastExam;  // Reasoning & Knowledge

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_mmlu_pro")
    private BigDecimal scoreMmluPro;  // Reasoning & Knowledge

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_gpqa_diamond")
    private BigDecimal scoreGpqaDiamond;  // Scientific Reasoning

    // Coding (2)
    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_livecode_bench")
    private BigDecimal scoreLivecodeBench;  // Coding

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_scicode")
    private BigDecimal scoreScicode;  // Coding

    // Specialized Skills (4)
    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_ifbench")
    private BigDecimal scoreIfbench;  // Instruction Following

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_math_500")
    private BigDecimal scoreMath500;  // Math 500

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aime")
    private BigDecimal scoreAime;  // AIME (Legacy version)

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aime_2025")
    private BigDecimal scoreAime2025;  // AIME 2025 (Competition Math)

    // Composite Indices (3)
    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aa_intelligence_index")
    private BigDecimal scoreAaIntelligenceIndex;  // Overall Intelligence Score

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aa_coding_index")
    private BigDecimal scoreAaCodingIndex;  // Coding Index (composite)

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aa_math_index")
    private BigDecimal scoreAaMathIndex;  // Math Index (composite)

    // Metadata
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
