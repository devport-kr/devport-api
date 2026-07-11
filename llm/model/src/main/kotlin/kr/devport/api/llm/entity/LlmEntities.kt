package kr.devport.api.llm.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import kr.devport.api.llm.enums.BenchmarkType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/** LLM model creator/provider (e.g., OpenAI, Anthropic, Alibaba). */
@Entity
@Table(name = "model_creators")
class ModelCreator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, length = 100, name = "external_id")
    var externalId: String? = null

    @Column(nullable = false, unique = true, length = 100)
    var slug: String = ""

    @Column(nullable = false, length = 200)
    var name: String = ""

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

@Entity
@Table(name = "llm_models")
class LLMModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(unique = true, length = 100, name = "external_id")
    var externalId: String? = null

    @Column(unique = true, length = 200)
    var slug: String? = null

    @Column(nullable = false, unique = true, length = 100, name = "model_id")
    var modelId: String = ""

    @Column(nullable = false, length = 200, name = "model_name")
    var modelName: String = ""

    @Column(name = "release_date")
    var releaseDate: LocalDate? = null

    @Column(length = 100)
    var provider: String? = null

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_creator_id")
    var modelCreator: ModelCreator? = null

    @Column(columnDefinition = "TEXT")
    var description: String? = null

    @Column(precision = 10, scale = 2, name = "price_input")
    var priceInput: BigDecimal? = null

    @Column(precision = 10, scale = 2, name = "price_output")
    var priceOutput: BigDecimal? = null

    @Column(precision = 10, scale = 2, name = "price_blended")
    var priceBlended: BigDecimal? = null

    @Column(name = "context_window")
    var contextWindow: Long? = null

    @Column(precision = 10, scale = 2, name = "output_speed_median")
    var outputSpeedMedian: BigDecimal? = null

    @Column(precision = 10, scale = 4, name = "latency_ttft")
    var latencyTtft: BigDecimal? = null

    @Column(precision = 10, scale = 4, name = "median_time_to_first_answer_token")
    var medianTimeToFirstAnswerToken: BigDecimal? = null

    @Column(length = 50)
    var license: String? = null

    @Column(precision = 5, scale = 2, name = "score_terminal_bench_hard")
    var scoreTerminalBenchHard: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_tau_bench_telecom")
    var scoreTauBenchTelecom: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_aa_lcr")
    var scoreAaLcr: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_humanitys_last_exam")
    var scoreHumanitysLastExam: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_mmlu_pro")
    var scoreMmluPro: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_gpqa_diamond")
    var scoreGpqaDiamond: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_livecode_bench")
    var scoreLivecodeBench: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_scicode")
    var scoreScicode: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_ifbench")
    var scoreIfbench: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_math_500")
    var scoreMath500: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_aime")
    var scoreAime: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_aime_2025")
    var scoreAime2025: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_aa_intelligence_index")
    var scoreAaIntelligenceIndex: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_aa_coding_index")
    var scoreAaCodingIndex: BigDecimal? = null

    @Column(precision = 5, scale = 2, name = "score_aa_math_index")
    var scoreAaMathIndex: BigDecimal? = null

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(nullable = false, name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

/** LLM 벤치마크 메타데이터. */
@Entity
@Table(name = "llm_benchmarks")
class LLMBenchmark {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "benchmark_type", length = 100)
    var benchmarkType: BenchmarkType? = null

    @Column(nullable = false, length = 200, name = "display_name")
    var displayName: String = ""

    @Column(nullable = false, length = 50, name = "category_group")
    var categoryGroup: String = ""

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String = ""

    @Column(columnDefinition = "TEXT")
    var explanation: String? = null

    @Column(name = "sort_order")
    var sortOrder: Int? = null

    @Column(nullable = false, name = "created_at")
    var createdAt: LocalDateTime? = null

    @PrePersist
    protected fun onCreate() {
        createdAt = LocalDateTime.now()
    }
}
