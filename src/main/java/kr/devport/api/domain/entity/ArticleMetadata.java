package kr.devport.api.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleMetadata {

    @Column(name = "stars")
    private Integer stars;

    @Column(name = "comments")
    private Integer comments;

    @Column(name = "upvotes")
    private Integer upvotes;

    @Column(name = "read_time", length = 50)
    private String readTime;

    @Column(name = "language", length = 50)
    private String language;
}
