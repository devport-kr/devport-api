package kr.devport.api.domain.wiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "wiki_drafts", indexes = {
        @Index(name = "idx_wiki_drafts_project_id", columnList = "project_id"),
        @Index(name = "idx_wiki_drafts_updated_at", columnList = "updated_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WikiDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections", columnDefinition = "jsonb")
    private List<Map<String, Object>> sections;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "current_counters", columnDefinition = "jsonb")
    private Map<String, Object> currentCounters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hidden_sections", columnDefinition = "jsonb")
    private List<String> hiddenSections;

    @Column(name = "source_published_version_id")
    private Long sourcePublishedVersionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Builder
    public WikiDraft(
            Long projectId,
            List<Map<String, Object>> sections,
            Map<String, Object> currentCounters,
            List<String> hiddenSections,
            Long sourcePublishedVersionId
    ) {
        this.projectId = projectId;
        this.sections = copySections(sections);
        this.currentCounters = copyMap(currentCounters);
        this.hiddenSections = hiddenSections == null ? null : new ArrayList<>(hiddenSections);
        this.sourcePublishedVersionId = sourcePublishedVersionId;
    }

    public void replaceSections(List<Map<String, Object>> sections) {
        this.sections = copySections(sections);
    }

    public void replaceCurrentCounters(Map<String, Object> currentCounters) {
        this.currentCounters = copyMap(currentCounters);
    }

    public void replaceHiddenSections(List<String> hiddenSections) {
        this.hiddenSections = hiddenSections == null ? null : new ArrayList<>(hiddenSections);
    }

    public List<Map<String, Object>> getSectionsOrEmpty() {
        return sections == null ? List.of() : sections;
    }

    public List<String> getHiddenSectionsOrEmpty() {
        return hiddenSections == null ? List.of() : hiddenSections;
    }

    private List<Map<String, Object>> copySections(List<Map<String, Object>> source) {
        if (source == null) {
            return null;
        }

        List<Map<String, Object>> copied = new ArrayList<>(source.size());
        for (Map<String, Object> section : source) {
            copied.add(section == null ? new LinkedHashMap<>() : new LinkedHashMap<>(section));
        }
        return copied;
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        return new LinkedHashMap<>(source);
    }
}
