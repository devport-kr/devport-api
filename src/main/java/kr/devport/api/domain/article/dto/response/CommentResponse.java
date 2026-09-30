package kr.devport.api.domain.article.dto.response;

import kr.devport.api.domain.article.entity.ArticleComment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private String id;
    private String content;
    private Boolean deleted;
    private String parentId;
    private CommentAuthorResponse author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isOwner;

    public static CommentResponse from(ArticleComment comment, Long currentUserId) {
        String displayContent = comment.getDeleted()
            ? "[삭제된 댓글입니다]"
            : comment.getContent();

        return CommentResponse.builder()
            .id(comment.getExternalId())
            .content(displayContent)
            .deleted(comment.getDeleted())
            .parentId(comment.getParentComment() != null
                ? comment.getParentComment().getExternalId()
                : null)
            .author(CommentAuthorResponse.from(comment.getUser()))
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .isOwner(currentUserId != null && currentUserId.equals(comment.getUser().getId()))
            .build();
    }
}
