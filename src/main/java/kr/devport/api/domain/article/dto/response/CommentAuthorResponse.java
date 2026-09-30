package kr.devport.api.domain.article.dto.response;

import kr.devport.api.domain.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentAuthorResponse {

    private Long id;
    private String name;
    private String profileImageUrl;
    private String flair;
    private String flairColor;

    public static CommentAuthorResponse from(User user) {
        return CommentAuthorResponse.builder()
            .id(user.getId())
            .name(user.getName())
            .profileImageUrl(user.getProfileImageUrl())
            .flair(user.getFlair())
            .flairColor(user.getFlairColor())
            .build();
    }
}
