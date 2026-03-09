package kr.devport.api.domain.wiki.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "wiki_chat_messages", indexes = {
        @Index(name = "idx_wiki_chat_messages_session_id", columnList = "session_id"),
        @Index(name = "idx_wiki_chat_messages_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WikiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private WikiChatSession session;

    @Column(name = "role", nullable = false, length = 10)
    private String role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_clarification", nullable = false)
    private boolean isClarification;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public WikiChatMessage(
            WikiChatSession session,
            String role,
            String content,
            boolean isClarification
    ) {
        this.session = session;
        this.role = role;
        this.content = content;
        this.isClarification = isClarification;
    }
}
