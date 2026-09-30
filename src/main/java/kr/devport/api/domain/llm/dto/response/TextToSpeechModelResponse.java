package kr.devport.api.domain.llm.dto.response;

import kr.devport.api.domain.llm.entity.TextToSpeechModel;
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
public class TextToSpeechModelResponse {

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

    public static TextToSpeechModelResponse fromEntity(TextToSpeechModel model) {
        if (model == null) {
            return null;
        }

        return TextToSpeechModelResponse.builder()
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
            .build();
    }
}
