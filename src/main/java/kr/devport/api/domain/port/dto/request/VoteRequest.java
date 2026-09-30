package kr.devport.api.domain.port.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Vote request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteRequest {

    @Schema(description = "Vote value: -1 (downvote), 0 (remove vote), 1 (upvote)", example = "1", required = true)
    @Min(value = -1, message = "Vote must be -1, 0, or 1")
    @Max(value = 1, message = "Vote must be -1, 0, or 1")
    private Integer vote;
}
