package kr.devport.api.dto.response;

import kr.devport.api.domain.entity.LLMModel;
import kr.devport.api.domain.enums.BenchmarkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** 특정 벤치마크 리더보드 응답 DTO. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMLeaderboardEntryResponse {

    private Long id;
    private String modelId;
    private String modelName;
    private String provider;
    private String license;

    private BigDecimal score;
    private Integer rank;

    private BigDecimal priceBlended;
    private Long contextWindow;

    public static LLMLeaderboardEntryResponse fromEntity(
        LLMModel model,
        BenchmarkType benchmarkType,
        BigDecimal score,
        Integer rank
    ) {
        return LLMLeaderboardEntryResponse.builder()
            .id(model.getId())
            .modelId(model.getModelId())
            .modelName(model.getModelName())
            .provider(model.getProvider())
            .license(model.getLicense())
            .score(score)
            .rank(rank)
            .priceBlended(model.getPriceBlended())
            .contextWindow(model.getContextWindow())
            .build();
    }
}
