package kr.devport.api.domain.port.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "Project summary response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSummaryResponse {

    @Schema(description = "Project external ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Project name", example = "ollama")
    private String name;

    @Schema(description = "Full repository name", example = "ollama/ollama")
    private String fullName;

    @Schema(description = "Total stars", example = "45000")
    private Integer stars;

    @Schema(description = "Stars gained this week", example = "500")
    private Integer starsWeekDelta;

    @Schema(description = "Primary programming language", example = "Go")
    private String language;

    @Schema(description = "Language badge color (hex)", example = "#00ADD8")
    private String languageColor;

    @Schema(description = "Number of releases in last 30 days", example = "3")
    private Integer releases30d;

    @Schema(description = "Sparkline data (last 30 days star count)", example = "[100, 105, 110, ...]")
    private List<Integer> sparklineData;
}
