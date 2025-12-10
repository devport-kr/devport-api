package kr.devport.api.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMModelCreateRequest {

    private String externalId;
    private String slug;

    @NotBlank(message = "Model ID is required")
    private String modelId;

    @NotBlank(message = "Model name is required")
    private String modelName;

    private LocalDate releaseDate;
    private String provider;
    private Long modelCreatorId;
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
}
