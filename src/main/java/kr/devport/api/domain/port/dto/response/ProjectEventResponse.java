package kr.devport.api.domain.port.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.devport.api.domain.port.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Schema(description = "Project event (release) response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEventResponse {

    @Schema(description = "Event external ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Release version", example = "v0.5.0")
    private String version;

    @Schema(description = "Release date", example = "2026-02-01")
    private LocalDate releasedAt;

    @Schema(description = "Event types", example = "[\"FEATURE\", \"FIX\"]")
    private Set<EventType> eventTypes;

    @Schema(description = "Korean summary", example = "주요 기능 업데이트 및 버그 수정")
    private String summary;

    @Schema(description = "Korean bullet points", example = "[\"새로운 모델 추가\", \"성능 개선\"]")
    private List<String> bullets;

    @Schema(description = "Impact score (0-100)", example = "75")
    private Integer impactScore;

    @Schema(description = "Is security-related", example = "false")
    private Boolean isSecurity;

    @Schema(description = "Contains breaking changes", example = "false")
    private Boolean isBreaking;

    @Schema(description = "Source URL (release notes)", example = "https://github.com/ollama/ollama/releases/tag/v0.5.0")
    private String sourceUrl;
}
