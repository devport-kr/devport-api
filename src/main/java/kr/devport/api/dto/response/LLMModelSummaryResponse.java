package kr.devport.api.dto.response;

import kr.devport.api.domain.entity.LLMModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** LLM 모델 목록 응답에 사용하는 요약 DTO. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMModelSummaryResponse {

    private Long id;
    private String slug;
    private String modelId;
    private String modelName;
    private LocalDate releaseDate;

    private String provider;
    private ModelCreatorResponse modelCreator;

    private BigDecimal scoreAaIntelligenceIndex;
    private BigDecimal priceBlended;
    private Long contextWindow;
    private BigDecimal outputSpeedMedian;
    private String license;

    private Integer rank;

    public static LLMModelSummaryResponse fromEntity(LLMModel model) {
        return LLMModelSummaryResponse.builder()
            .id(model.getId())
            .slug(model.getSlug())
            .modelId(model.getModelId())
            .modelName(model.getModelName())
            .releaseDate(model.getReleaseDate())
            .provider(model.getProvider())
            .modelCreator(ModelCreatorResponse.from(model.getModelCreator()))
            .scoreAaIntelligenceIndex(model.getScoreAaIntelligenceIndex())
            .priceBlended(model.getPriceBlended())
            .contextWindow(model.getContextWindow())
            .outputSpeedMedian(model.getOutputSpeedMedian())
            .license(model.getLicense())
            .build();
    }

    public static LLMModelSummaryResponse fromEntity(LLMModel model, Integer rank) {
        LLMModelSummaryResponse response = fromEntity(model);
        response.setRank(rank);
        return response;
    }
}
