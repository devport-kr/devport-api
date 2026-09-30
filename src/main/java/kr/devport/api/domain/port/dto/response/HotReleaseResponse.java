package kr.devport.api.domain.port.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.devport.api.domain.port.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Schema(description = "Hot release (high-impact event) response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotReleaseResponse {

    @Schema(description = "Event external ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Project name", example = "ollama")
    private String projectName;

    @Schema(description = "Release version", example = "v0.5.0")
    private String version;

    @Schema(description = "Release date", example = "2026-02-01")
    private LocalDate releasedAt;

    @Schema(description = "Event types", example = "[\"FEATURE\", \"SECURITY\"]")
    private Set<EventType> eventTypes;

    @Schema(description = "Korean summary", example = "새로운 멀티모달 모델 지원 추가")
    private String summary;

    @Schema(description = "Impact score (0-100)", example = "85")
    private Integer impactScore;

    @Schema(description = "Is security-related", example = "false")
    private Boolean isSecurity;

    @Schema(description = "Contains breaking changes", example = "false")
    private Boolean isBreaking;
}
