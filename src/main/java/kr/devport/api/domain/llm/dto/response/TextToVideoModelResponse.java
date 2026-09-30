package kr.devport.api.domain.llm.dto.response;

import kr.devport.api.domain.llm.entity.TextToVideoModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextToVideoModelResponse {

    private Long id;
    private String externalId;
    private String slug;
    private String name;
    private ModelCreatorResponse modelCreator;

    private BigDecimal elo;
    private Integer rank;
    private String ci95;
    private Integer appearances;
    private String releaseDate;

    private List<TextToVideoCategoryResponse> categories;

    public static TextToVideoModelResponse fromEntity(TextToVideoModel model) {
        if (model == null) {
            return null;
        }

        List<TextToVideoCategoryResponse> categoryResponses = model.getCategories() == null
            ? Collections.emptyList()
            : model.getCategories().stream()
                .map(TextToVideoCategoryResponse::fromEntity)
                .collect(Collectors.toList());

        return TextToVideoModelResponse.builder()
            .id(model.getId())
            .externalId(model.getExternalId())
            .slug(model.getSlug())
            .name(model.getName())
            .modelCreator(ModelCreatorResponse.from(model.getModelCreator()))
            .elo(model.getElo())
            .rank(model.getRank())
            .ci95(model.getCi95())
            .appearances(model.getAppearances())
            .releaseDate(model.getReleaseDate())
            .categories(categoryResponses)
            .build();
    }
}
