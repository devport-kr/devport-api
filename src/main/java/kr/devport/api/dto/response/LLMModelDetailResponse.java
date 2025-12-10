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

/** 상세 조회용 LLM 모델 응답 DTO. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMModelDetailResponse {

    private Long id;
    private String externalId;
    private String slug;
    private String modelId;
    private String modelName;
    private LocalDate releaseDate;

    private String provider;
    private ModelCreatorResponse modelCreator;

    private String description;

    private BigDecimal priceInput;
    private BigDecimal priceOutput;
    private BigDecimal priceBlended;

    private Long contextWindow;
    private BigDecimal outputSpeedMedian;
    private BigDecimal latencyTtft;
    private BigDecimal medianTimeToFirstAnswerToken;
    private String license;

    private BigDecimal scoreTerminalBenchHard;
    private BigDecimal scoreTauBenchTelecom;

    private BigDecimal scoreAaLcr;
    private BigDecimal scoreHumanitysLastExam;
    private BigDecimal scoreMmluPro;
    private BigDecimal scoreGpqaDiamond;

    private BigDecimal scoreLivecodeBench;
    private BigDecimal scoreScicode;

    private BigDecimal scoreIfbench;
    private BigDecimal scoreMath500;
    private BigDecimal scoreAime;
    private BigDecimal scoreAime2025;

    private BigDecimal scoreAaIntelligenceIndex;
    private BigDecimal scoreAaCodingIndex;
    private BigDecimal scoreAaMathIndex;

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
