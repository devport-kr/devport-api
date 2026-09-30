package kr.devport.api.domain.port.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Project detail response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDetailResponse {

    @Schema(description = "Project external ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Project name", example = "ollama")
    private String name;

    @Schema(description = "Full repository name", example = "ollama/ollama")
    private String fullName;

    @Schema(description = "Repository URL", example = "https://github.com/ollama/ollama")
    private String repoUrl;

    @Schema(description = "Homepage URL", example = "https://ollama.ai")
    private String homepageUrl;

    @Schema(description = "Project description", example = "Get up and running with large language models locally")
    private String description;

    @Schema(description = "Total stars", example = "45000")
    private Integer stars;

    @Schema(description = "Total forks", example = "3000")
    private Integer forks;

    @Schema(description = "Number of contributors", example = "200")
    private Integer contributors;

    @Schema(description = "Primary programming language", example = "Go")
    private String language;

    @Schema(description = "Language badge color (hex)", example = "#00ADD8")
    private String languageColor;

    @Schema(description = "License", example = "MIT")
    private String license;

    @Schema(description = "Last release date", example = "2026-02-01")
    private LocalDate lastRelease;

    @Schema(description = "Project tags", example = "[\"llm\", \"ai\", \"local\"]")
    private List<String> tags;
}
