package kr.devport.api.domain.port.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "Vote response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteResponse {

    @Schema(description = "Total vote score", example = "5")
    private Integer votes;

    @Schema(description = "Current user's vote (-1, 0, or 1)", example = "1")
    private Integer userVote;
}
