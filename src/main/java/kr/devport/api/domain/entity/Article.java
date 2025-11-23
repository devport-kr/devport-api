package kr.devport.api.domain.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import kr.devport.api.domain.enums.Category;
import kr.devport.api.domain.enums.ItemType;
import kr.devport.api.domain.enums.Source;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "item_type")
    private ItemType itemType;  // REPO, BLOG, DISCUSSION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;  // github, hackernews, reddit, medium, devto, hashnode

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;  // AI_LLM, DEVOPS_SRE, BACKEND, INFRA_CLOUD, OTHER

    @Column(nullable = false, length = 500, name = "summary_ko_title")
    private String summaryKoTitle;  // Korean one-line summary title

    @Column(columnDefinition = "TEXT", name = "summary_ko_body")
    private String summaryKoBody;  // Korean detailed summary (optional)

    @Column(nullable = false, length = 500, name = "title_en")
    private String titleEn;  // Original English title

    @Column(nullable = false, length = 1000)
    private String url;  // Link to original article/repo

    @Column(nullable = false)
    private Integer score;  // Popularity score for ranking

    @ElementCollection
    @CollectionTable(name = "article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();  // Technical tags (e.g., ["rust", "api-gateway", "performance"])

    @Column(nullable = false, name = "created_at_source")
    private LocalDateTime createdAtSource;  // When the article was published at source

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;  // When we scraped/created this record

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @Embedded
    private ArticleMetadata metadata;  // Metadata (embedded object)
}
