package kr.devport.api.domain.llm.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMModelSearchCondition {

    // Existing filters
    private String provider;
    private String creatorSlug;
    private String license;
    private BigDecimal maxPrice;
    private Long minContextWindow;

    // New filters for enhanced search
    private String keyword;              // modelName LIKE search
    private LocalDate releaseDateFrom;
    private LocalDate releaseDateTo;
    private BigDecimal minScore;         // minimum intelligence index score
}
