package kr.devport.api.domain.llm.dto.response;

import kr.devport.api.domain.llm.entity.TextToVideoCategory;
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
public class TextToVideoCategoryResponse {

    private String styleCategory;
    private String subjectMatterCategory;
    private String formatCategory;
    private BigDecimal elo;
    private String ci95;
    private Integer appearances;

    public static TextToVideoCategoryResponse fromEntity(TextToVideoCategory category) {
        if (category == null) {
            return null;
        }

        return TextToVideoCategoryResponse.builder()
            .styleCategory(category.getStyleCategory())
            .subjectMatterCategory(category.getSubjectMatterCategory())
            .formatCategory(category.getFormatCategory())
            .elo(category.getElo())
            .ci95(category.getCi95())
            .appearances(category.getAppearances())
            .build();
    }
}
