package kr.devport.api.dto.response;

import kr.devport.api.domain.entity.ModelCreator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 모델 제공자 정보를 담는 응답 DTO. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelCreatorResponse {

    private Long id;
    private String externalId;
    private String slug;
    private String name;

    public static ModelCreatorResponse from(ModelCreator modelCreator) {
        if (modelCreator == null) {
            return null;
        }

        return ModelCreatorResponse.builder()
                .id(modelCreator.getId())
                .externalId(modelCreator.getExternalId())
                .slug(modelCreator.getSlug())
                .name(modelCreator.getName())
                .build();
    }
}
