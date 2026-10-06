package kr.devport.api.domain.newsletter.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyDigestArticleResponse {

    private String externalId;
    private String title;
    private String source;
    private int votes;
    private int comments;
    private String articleUrl;
    private String originalUrl;
}
