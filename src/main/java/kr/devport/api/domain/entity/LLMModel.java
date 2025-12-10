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

    @Column(unique = true, length = 100, name = "external_id")
    private String externalId;

    @Column(unique = true, length = 200)
    private String slug;

    @NotBlank
    @Column(nullable = false, unique = true, length = 100, name = "model_id")
    private String modelId;

    @NotBlank
    @Column(nullable = false, length = 200, name = "model_name")
    private String modelName;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(length = 100)
    private String provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_creator_id")
    private ModelCreator modelCreator;

    @Column(columnDefinition = "TEXT")
    private String description;

    @DecimalMin("0.00")
    @Column(precision = 10, scale = 2, name = "price_input")
    private BigDecimal priceInput;

    @DecimalMin("0.00")
    @Column(precision = 10, scale = 2, name = "price_output")
    private BigDecimal priceOutput;

    @DecimalMin("0.00")
    @Column(precision = 10, scale = 2, name = "price_blended")
    private BigDecimal priceBlended;

    @Min(1)
    @Column(name = "context_window")
    private Long contextWindow;

    @DecimalMin("0.01")
    @Column(precision = 10, scale = 2, name = "output_speed_median")
    private BigDecimal outputSpeedMedian;

    @DecimalMin("0.0001")
    @Column(precision = 10, scale = 4, name = "latency_ttft")
    private BigDecimal latencyTtft;

    @DecimalMin("0.0001")
    @Column(precision = 10, scale = 4, name = "median_time_to_first_answer_token")
    private BigDecimal medianTimeToFirstAnswerToken;

    @Column(length = 50)
    private String license;

    // 벤치마크 점수 0~100 범위, 미측정 항목들은 null 로 저장.
    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_terminal_bench_hard")
    private BigDecimal scoreTerminalBenchHard;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_tau_bench_telecom")
    private BigDecimal scoreTauBenchTelecom;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aa_lcr")
    private BigDecimal scoreAaLcr;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_humanitys_last_exam")
    private BigDecimal scoreHumanitysLastExam;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_mmlu_pro")
    private BigDecimal scoreMmluPro;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_gpqa_diamond")
    private BigDecimal scoreGpqaDiamond;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_livecode_bench")
    private BigDecimal scoreLivecodeBench;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_scicode")
    private BigDecimal scoreScicode;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_ifbench")
    private BigDecimal scoreIfbench;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_math_500")
    private BigDecimal scoreMath500;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aime")
    private BigDecimal scoreAime;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aime_2025")
    private BigDecimal scoreAime2025;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aa_intelligence_index")
    private BigDecimal scoreAaIntelligenceIndex;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aa_coding_index")
    private BigDecimal scoreAaCodingIndex;

    @Min(0) @Max(100)
    @Column(precision = 5, scale = 2, name = "score_aa_math_index")
    private BigDecimal scoreAaMathIndex;

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
