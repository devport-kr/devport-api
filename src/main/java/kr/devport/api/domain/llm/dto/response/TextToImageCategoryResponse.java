package kr.devport.api.domain.llm.dto.response;

import kr.devport.api.domain.llm.entity.TextToImageCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextToImageCategoryResponse {

    private String styleCategory;
    private String subjectMatterCategory;
    private BigDecimal elo;
    private String ci95;
    private Integer appearances;

    public static TextToImageCategoryResponse fromEntity(TextToImageCategory category) {
        if (category == null) {
            return null;
        }

        return TextToImageCategoryResponse.builder()
            .styleCategory(category.getStyleCategory())
            .subjectMatterCategory(category.getSubjectMatterCategory())
            .elo(category.getElo())
            .ci95(category.getCi95())
            .appearances(category.getAppearances())
            .build();
    }
}
