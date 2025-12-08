package kr.devport.api.dto.response;

import kr.devport.api.domain.entity.LLMModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Complete model details with all benchmark scores (18 total)
 * Used for /api/llm/models/{modelId} endpoint
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMModelDetailResponse {

    // Model identification
    private Long id;
    private String externalId;  // UUID from API
    private String slug;  // URL-friendly identifier
    private String modelId;
    private String modelName;
    private LocalDate releaseDate;

    // Provider information
    private String provider;  // Legacy field (can be null)
    private ModelCreatorResponse modelCreator;  // Detailed creator info from API

    private String description;

    // Pricing (USD per million tokens)
    private BigDecimal priceInput;
    private BigDecimal priceOutput;
    private BigDecimal priceBlended;

    // Performance metrics
    private Long contextWindow;
    private BigDecimal outputSpeedMedian;  // tokens/sec
    private BigDecimal latencyTtft;  // Time to first token (seconds)
    private BigDecimal medianTimeToFirstAnswerToken;  // Time to first answer token (seconds)
    private String license;

    // Benchmark Scores (all 15 categories)
    // Agentic Capabilities
    private BigDecimal scoreTerminalBenchHard;
    private BigDecimal scoreTauBenchTelecom;

    // Reasoning & Knowledge
    private BigDecimal scoreAaLcr;
    private BigDecimal scoreHumanitysLastExam;
    private BigDecimal scoreMmluPro;
    private BigDecimal scoreGpqaDiamond;

    // Coding
    private BigDecimal scoreLivecodeBench;
    private BigDecimal scoreScicode;

    // Specialized Skills
    private BigDecimal scoreIfbench;
    private BigDecimal scoreMath500;
    private BigDecimal scoreAime;
    private BigDecimal scoreAime2025;

    // Composite Indices
    private BigDecimal scoreAaIntelligenceIndex;
    private BigDecimal scoreAaCodingIndex;
    private BigDecimal scoreAaMathIndex;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LLMModelDetailResponse fromEntity(LLMModel model) {
        return LLMModelDetailResponse.builder()
            .id(model.getId())
            .externalId(model.getExternalId())
            .slug(model.getSlug())
            .modelId(model.getModelId())
            .modelName(model.getModelName())
            .releaseDate(model.getReleaseDate())
            .provider(model.getProvider())
            .modelCreator(ModelCreatorResponse.from(model.getModelCreator()))
            .description(model.getDescription())
            .priceInput(model.getPriceInput())
            .priceOutput(model.getPriceOutput())
            .priceBlended(model.getPriceBlended())
            .contextWindow(model.getContextWindow())
            .outputSpeedMedian(model.getOutputSpeedMedian())
            .latencyTtft(model.getLatencyTtft())
            .medianTimeToFirstAnswerToken(model.getMedianTimeToFirstAnswerToken())
            .license(model.getLicense())
            .scoreTerminalBenchHard(model.getScoreTerminalBenchHard())
            .scoreTauBenchTelecom(model.getScoreTauBenchTelecom())
            .scoreAaLcr(model.getScoreAaLcr())
            .scoreHumanitysLastExam(model.getScoreHumanitysLastExam())
            .scoreMmluPro(model.getScoreMmluPro())
            .scoreGpqaDiamond(model.getScoreGpqaDiamond())
            .scoreLivecodeBench(model.getScoreLivecodeBench())
            .scoreScicode(model.getScoreScicode())
            .scoreIfbench(model.getScoreIfbench())
            .scoreMath500(model.getScoreMath500())
            .scoreAime(model.getScoreAime())
            .scoreAime2025(model.getScoreAime2025())
            .scoreAaIntelligenceIndex(model.getScoreAaIntelligenceIndex())
            .scoreAaCodingIndex(model.getScoreAaCodingIndex())
            .scoreAaMathIndex(model.getScoreAaMathIndex())
            .createdAt(model.getCreatedAt())
            .updatedAt(model.getUpdatedAt())
            .build();
    }
}
