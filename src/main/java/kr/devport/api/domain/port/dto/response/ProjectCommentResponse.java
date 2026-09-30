package kr.devport.api.domain.port.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.devport.api.domain.article.dto.response.CommentAuthorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "Project comment response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCommentResponse {

    @Schema(description = "Comment external ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Comment content", example = "Great project!")
    private String content;

    @Schema(description = "Is deleted (soft delete)", example = "false")
    private Boolean deleted;

    @Schema(description = "Parent comment ID (for replies)", example = "123e4567-e89b-12d3-a456-426614174001")
    private String parentId;

    @Schema(description = "Comment author with flair")
    private CommentAuthorResponse author;

    @Schema(description = "Vote score (upvotes - downvotes)", example = "5")
    private Integer votes;

    @Schema(description = "Current user's vote (-1, 0, or 1)", example = "1")
    private Integer userVote;

    @Schema(description = "Created timestamp", example = "2026-02-01T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Updated timestamp", example = "2026-02-01T10:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Is current user the comment owner", example = "false")
    private Boolean isOwner;
}
