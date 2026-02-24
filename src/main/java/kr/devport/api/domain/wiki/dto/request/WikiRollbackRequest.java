package kr.devport.api.domain.wiki.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WikiRollbackRequest {

    @NotNull(message = "targetVersionNumber is required")
    @Min(value = 1, message = "targetVersionNumber must be greater than 0")
    private Integer targetVersionNumber;
}
