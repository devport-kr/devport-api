package kr.devport.api.domain.llm.dto.request.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCreatorUpdateRequest {

    private String externalId;
    private String slug;
    private String name;
}
