package kr.devport.api.domain.article.entity;

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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import kr.devport.api.domain.article.enums.Category;
import kr.devport.api.domain.article.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "articles", indexes = {
    @Index(name = "idx_articles_summary_ko_title", columnList = "summary_ko_title"),
    @Index(name = "idx_articles_created_at_source", columnList = "created_at_source")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100, name = "external_id")
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "item_type")
    private ItemType itemType;

    @Column(nullable = false, length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false, length = 500, name = "summary_ko_title")
    private String summaryKoTitle;

    @Column(columnDefinition = "TEXT", name = "summary_ko_body")
    private String summaryKoBody;

    @Column(nullable = false, length = 500, name = "title_en")
    private String titleEn;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private Integer score;

    // Batch-load tags for a page of articles (1 query instead of N).
    @BatchSize(size = 100)
    @ElementCollection
    @CollectionTable(name = "article_tags", joinColumns = @JoinColumn(name = "article_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false, name = "created_at_source")
    private LocalDateTime createdAtSource;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    @Embedded
    private ArticleMetadata metadata;

    @PrePersist
    public void generateExternalId() {
        if (this.externalId == null) {
            this.externalId = UUID.randomUUID().toString();
        }
    }
}
