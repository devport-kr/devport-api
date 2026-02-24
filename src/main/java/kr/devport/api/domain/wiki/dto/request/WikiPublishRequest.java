package kr.devport.api.domain.wiki.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiPublishRequest {

    @NotNull(message = "draftId is required")
    private Long draftId;
}
