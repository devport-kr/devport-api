package kr.devport.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleMetadataResponse {

    private Integer stars;
    private Integer comments;
    private Integer upvotes;
    private String readTime;
    private String language;
}
