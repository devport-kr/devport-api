package kr.devport.api.domain.wiki.entity;

import jakarta.persistence.*;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.wiki.enums.WikiChatSessionType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "wiki_chat_sessions", indexes = {
        @Index(name = "idx_wiki_chat_sessions_user_id", columnList = "user_id"),
        @Index(name = "idx_wiki_chat_sessions_project", columnList = "project_external_id"),
        @Index(name = "idx_wiki_chat_sessions_expires_at", columnList = "expires_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WikiChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "project_external_id", length = 255)
    private String projectExternalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_type", nullable = false, length = 20)
    private WikiChatSessionType sessionType;

    @Setter
    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Setter
    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    @Setter
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public WikiChatSession(
            String externalId,
            User user,
            String projectExternalId,
            WikiChatSessionType sessionType,
            String title,
            LocalDateTime lastMessageAt,
            LocalDateTime expiresAt
    ) {
        this.externalId = externalId;
        this.user = user;
        this.projectExternalId = projectExternalId;
        this.sessionType = sessionType;
        this.title = title;
        this.lastMessageAt = lastMessageAt;
        this.expiresAt = expiresAt;
    }
}
