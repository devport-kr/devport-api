package kr.devport.api.domain.newsletter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 회원 1명당 최대 1개의 뉴스레터 구독.
 * 구독 해지 시 행을 삭제해 이메일을 보관하지 않는다.
 *
 * <p>회원은 연관관계가 아닌 user_id 값으로만 참조한다. prod native image는 런타임에 Hibernate 프록시를
 * 만들 수 없어(BytecodeProvider 'none') LAZY 연관이나 getReferenceById가 실패한다.
 * users 삭제 시 함께 지워지는 것은 DB의 FK(ON DELETE CASCADE)가 보장한다.
 */
@Entity
@Table(name = "newsletter_subscriptions", indexes = {
    @Index(name = "idx_newsletter_subscriptions_email", columnList = "email"),
    @Index(name = "idx_newsletter_subscriptions_status", columnList = "status, id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NewsletterSubscriptionStatus status;

    /** 인증 링크 토큰의 SHA-256 hex. 원문은 메일에만 존재한다. */
    @Column(name = "verification_token_hash", unique = true, length = 64)
    private String verificationTokenHash;

    @Column(name = "verification_expires_at")
    private LocalDateTime verificationExpiresAt;

    @Column(name = "verification_sent_at")
    private LocalDateTime verificationSentAt;

    /** 메일마다 들어가는 수신거부 링크 토큰 */
    @Column(name = "unsubscribe_token", nullable = false, unique = true, length = 64)
    private String unsubscribeToken;

    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isVerificationExpired() {
        return verificationExpiresAt == null || LocalDateTime.now().isAfter(verificationExpiresAt);
    }
}
