package kr.devport.api.domain.port.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import kr.devport.api.domain.port.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "project_events", indexes = {
    @Index(name = "idx_project_events_project_released", columnList = "project_id,released_at"),
    @Index(name = "idx_project_events_security", columnList = "is_security"),
    @Index(name = "idx_project_events_breaking", columnList = "is_breaking")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false, length = 100)
    private String externalId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "released_at", nullable = false)
    private LocalDate releasedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_event_types", joinColumns = @JoinColumn(name = "event_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    @Builder.Default
    private Set<EventType> eventTypes = new HashSet<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @ElementCollection
    @CollectionTable(name = "project_event_bullets", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "bullet", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> bullets = new ArrayList<>();

    @Column(name = "impact_score")
    private Integer impactScore;

    @Column(name = "is_security")
    @Builder.Default
    private Boolean isSecurity = false;

    @Column(name = "is_breaking")
    @Builder.Default
    private Boolean isBreaking = false;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "raw_notes", columnDefinition = "TEXT")
    private String rawNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (externalId == null) {
            externalId = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
    }
}
