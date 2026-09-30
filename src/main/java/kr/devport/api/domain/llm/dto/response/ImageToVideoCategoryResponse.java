package kr.devport.api.domain.llm.dto.response;

import kr.devport.api.domain.llm.entity.ImageToVideoCategory;
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
public class ImageToVideoCategoryResponse {

    private String styleCategory;
    private String subjectMatterCategory;
    private String formatCategory;
    private BigDecimal elo;
    private String ci95;
    private Integer appearances;

    public static ImageToVideoCategoryResponse fromEntity(ImageToVideoCategory category) {
        if (category == null) {
            return null;
        }

        return ImageToVideoCategoryResponse.builder()
            .styleCategory(category.getStyleCategory())
            .subjectMatterCategory(category.getSubjectMatterCategory())
            .formatCategory(category.getFormatCategory())
            .elo(category.getElo())
            .ci95(category.getCi95())
            .appearances(category.getAppearances())
            .build();
    }
}
