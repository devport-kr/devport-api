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
    private Integer stars;  // For GitHub repos

    @Column(name = "comments")
    private Integer comments;  // For HN, Reddit

    @Column(name = "upvotes")
    private Integer upvotes;  // For Reddit

    @Column(name = "read_time", length = 50)
    private String readTime;  // e.g., "5분", "12분" (for blog posts)

    @Column(name = "language", length = 50)
    private String language;  // Spoken language: "English" or "Chinese" (for GitHub repos)
}
