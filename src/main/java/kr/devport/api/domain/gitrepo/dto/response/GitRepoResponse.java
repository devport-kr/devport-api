package kr.devport.api.domain.gitrepo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.devport.api.domain.gitrepo.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "GitHub Repository response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitRepoResponse {

    @Schema(description = "Repository ID", example = "1")
    private Long id;

    @Schema(description = "Full repository name", example = "facebook/react")
    private String fullName;

    @Schema(description = "Repository URL", example = "https://github.com/facebook/react")
    private String url;

    @Schema(description = "Repository description", example = "A declarative, efficient, and flexible JavaScript library for building user interfaces.")
    private String description;

    @Schema(description = "Programming language", example = "JavaScript")
    private String language;

    @Schema(description = "Total stars", example = "220000")
    private Integer stars;

    @Schema(description = "Total forks", example = "45000")
    private Integer forks;

    @Schema(description = "Stars gained this week", example = "1250")
    private Integer starsThisWeek;

    @Schema(description = "Korean title summary", example = "React - UI 구축을 위한 JavaScript 라이브러리")
    private String summaryKoTitle;

    @Schema(description = "Korean body summary", example = "Facebook에서 개발한 사용자 인터페이스 구축용 JavaScript 라이브러리입니다.")
    private String summaryKoBody;

    @Schema(description = "Technology category", example = "FRONTEND")
    private Category category;

    @Schema(description = "Trending score", example = "9850")
    private Integer score;

    @Schema(description = "Created timestamp", example = "2025-01-15T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp", example = "2025-01-16T14:30:00")
    private LocalDateTime updatedAt;
}
