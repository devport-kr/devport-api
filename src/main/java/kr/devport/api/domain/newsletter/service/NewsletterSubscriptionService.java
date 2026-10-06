package kr.devport.api.domain.newsletter.service;

import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.auth.service.TurnstileService;
import kr.devport.api.domain.common.exception.BotVerificationException;
import kr.devport.api.domain.common.exception.DuplicateEmailException;
import kr.devport.api.domain.common.exception.ServiceUnavailableException;
import kr.devport.api.domain.common.exception.TokenExpiredException;
import kr.devport.api.domain.common.exception.TokenNotFoundException;
import kr.devport.api.domain.common.exception.TooManyRequestsException;
import kr.devport.api.domain.common.logging.LogSanitizer;
import kr.devport.api.domain.common.ratelimit.RedisRateLimiter;
import kr.devport.api.domain.newsletter.dto.request.NewsletterSubscribeRequest;
import kr.devport.api.domain.newsletter.dto.response.NewsletterActionResponse;
import kr.devport.api.domain.newsletter.dto.response.NewsletterSubscriptionResponse;
import kr.devport.api.domain.newsletter.entity.NewsletterSubscription;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import kr.devport.api.domain.newsletter.repository.NewsletterSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * 회원 뉴스레터 구독 관리 (double opt-in).
 * 구독 신청 → 인증 메일 → 링크에서 확인해야 ACTIVE가 되어 발송 대상이 된다.
 * 구독 해지 시 행을 삭제해 이메일을 더 보관하지 않는다.
 *
 * <p>인증 메일 발송은 남의 메일함 폭탄/SES 평판 훼손에 악용될 수 있으므로
 * 매 요청 Turnstile 검증 + 사용자·IP·수신 이메일·전체 단위 한도를 두고, Redis 장애 시에는 발송하지 않는다(fail-closed).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterSubscriptionService {

    static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration HOURLY = Duration.ofHours(1);
    private static final Duration DAILY = Duration.ofHours(24);
    private static final int VERIFICATION_LIMIT_PER_USER_PER_HOUR = 5;
    private static final int VERIFICATION_LIMIT_PER_IP_PER_HOUR = 10;
    private static final int VERIFICATION_LIMIT_PER_EMAIL_PER_DAY = 3;
    private static final String TOO_MANY_MESSAGE = "인증 메일 요청이 너무 많습니다. 1시간 후 다시 시도해주세요.";

    private final NewsletterSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final NewsletterMailService mailService;
    private final RedisRateLimiter rateLimiter;
    private final TurnstileService turnstileService;

    /** 사이트 전체 인증 메일 시간당 상한. 정상 사용량보다 충분히 크게, 공격 시 SES 평판을 지킬 만큼 작게. */
    @Value("${app.newsletter.verification-global-limit-per-hour:200}")
    private int globalVerificationLimitPerHour;

    @Transactional(readOnly = true)
    public NewsletterSubscriptionResponse getMySubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
            .map(NewsletterSubscriptionResponse::from)
            .orElseGet(NewsletterSubscriptionResponse::none);
    }

    /**
     * 구독 신청(또는 인증 메일 재발송, 이메일 변경).
     * 이미 같은 이메일로 ACTIVE면 아무것도 하지 않는다. 이메일을 바꾸면 다시 인증해야 한다.
     */
    @Transactional
    public NewsletterSubscriptionResponse subscribe(Long userId, NewsletterSubscribeRequest request, String clientIp) {
        String email = normalizeEmail(request.getEmail());
        NewsletterSubscription subscription = subscriptionRepository.findByUserId(userId).orElse(null);

        if (subscription != null
            && subscription.getStatus() == NewsletterSubscriptionStatus.ACTIVE
            && subscription.getEmail().equals(email)) {
            return NewsletterSubscriptionResponse.from(subscription);
        }

        if (subscriptionRepository.existsByEmailAndStatusAndUserIdNot(email, NewsletterSubscriptionStatus.ACTIVE, userId)) {
            throw new DuplicateEmailException("이미 다른 계정에서 구독 중인 이메일입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (subscription != null
            && subscription.getVerificationSentAt() != null
            && subscription.getVerificationSentAt().isAfter(now.minus(RESEND_COOLDOWN))) {
            throw new TooManyRequestsException("인증 메일은 1분에 한 번만 보낼 수 있습니다. 잠시 후 다시 시도해주세요.");
        }

        if (!turnstileService.validateToken(request.getTurnstileToken(), clientIp)) {
            log.warn("Turnstile validation failed for newsletter subscribe, userId={}, clientIp={}",
                userId, LogSanitizer.maskIp(clientIp));
            throw new BotVerificationException("Bot verification failed");
        }

        // 좁은 범위부터 검사해서, 앞에서 거부된 요청이 전체 한도를 소모하지 않게 한다.
        requirePermit("newsletter:verify:user:" + userId, VERIFICATION_LIMIT_PER_USER_PER_HOUR, HOURLY, TOO_MANY_MESSAGE);
        requirePermit("newsletter:verify:ip:" + clientIp, VERIFICATION_LIMIT_PER_IP_PER_HOUR, HOURLY, TOO_MANY_MESSAGE);
        requirePermit("newsletter:verify:email:" + NewsletterTokens.sha256(email), VERIFICATION_LIMIT_PER_EMAIL_PER_DAY, DAILY,
            "이 이메일로 인증 메일을 너무 많이 보냈습니다. 내일 다시 시도해주세요.");
        try {
            requirePermit("newsletter:verify:global", globalVerificationLimitPerHour, HOURLY,
                "지금은 인증 메일 요청이 많아 처리할 수 없습니다. 잠시 후 다시 시도해주세요.");
        } catch (TooManyRequestsException e) {
            log.warn("Newsletter verification global limit ({}/h) reached — possible abuse", globalVerificationLimitPerHour);
            throw e;
        }

        if (subscription == null) {
            subscription = NewsletterSubscription.builder()
                .user(userRepository.getReferenceById(userId))
                .unsubscribeToken(NewsletterTokens.generate())
                .createdAt(now)
                .build();
        }

        String rawToken = NewsletterTokens.generate();
        subscription.setEmail(email);
        subscription.setStatus(NewsletterSubscriptionStatus.PENDING);
        subscription.setVerificationTokenHash(NewsletterTokens.sha256(rawToken));
        subscription.setVerificationExpiresAt(now.plus(VERIFICATION_TTL));
        subscription.setVerificationSentAt(now);
        subscription.setConsentedAt(now);
        subscription.setVerifiedAt(null);
        subscription.setUpdatedAt(now);
        subscription = subscriptionRepository.save(subscription);

        // 발송 실패 시 EmailDeliveryException → 트랜잭션 롤백
        mailService.sendVerificationEmail(email, rawToken);
        log.info("Newsletter verification requested for userId={}, email={}", userId, LogSanitizer.maskEmail(email));

        return NewsletterSubscriptionResponse.from(subscription);
    }

    @Transactional
    public NewsletterActionResponse confirm(String rawToken) {
        NewsletterSubscription subscription = subscriptionRepository
            .findByVerificationTokenHash(NewsletterTokens.sha256(rawToken))
            .orElseThrow(() -> new TokenNotFoundException("유효하지 않거나 이미 사용된 인증 링크입니다."));

        if (subscription.isVerificationExpired()) {
            throw new TokenExpiredException("인증 링크가 만료되었습니다. 마이페이지에서 다시 구독을 신청해주세요.");
        }

        if (subscriptionRepository.existsByEmailAndStatusAndUserIdNot(
            subscription.getEmail(), NewsletterSubscriptionStatus.ACTIVE, subscription.getUser().getId())) {
            throw new DuplicateEmailException("이미 다른 계정에서 구독 중인 이메일입니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        subscription.setStatus(NewsletterSubscriptionStatus.ACTIVE);
        subscription.setVerifiedAt(now);
        subscription.setVerificationTokenHash(null);
        subscription.setVerificationExpiresAt(null);
        subscription.setUpdatedAt(now);
        subscriptionRepository.save(subscription);

        mailService.sendSubscribedNotice(subscription);
        log.info("Newsletter subscription activated, subscriptionId={}", subscription.getId());

        return NewsletterActionResponse.builder()
            .message("뉴스레터 구독이 완료되었습니다.")
            .email(LogSanitizer.maskEmail(subscription.getEmail()))
            .build();
    }

    /** 로그인한 회원 본인의 구독 해지. 구독이 없으면 아무것도 하지 않는다. */
    @Transactional
    public void unsubscribe(Long userId) {
        subscriptionRepository.findByUserId(userId).ifPresent(this::remove);
    }

    /** 메일 속 수신거부 링크로 해지 */
    @Transactional
    public NewsletterActionResponse unsubscribeByToken(String unsubscribeToken) {
        NewsletterSubscription subscription = subscriptionRepository.findByUnsubscribeToken(unsubscribeToken)
            .orElseThrow(() -> new TokenNotFoundException("이미 수신 거부되었거나 유효하지 않은 링크입니다."));
        remove(subscription);
        return NewsletterActionResponse.builder()
            .message("뉴스레터 수신 거부가 처리되었습니다.")
            .email(LogSanitizer.maskEmail(subscription.getEmail()))
            .build();
    }

    /** RFC 8058 one-click: 메일 클라이언트가 호출하므로 없는 토큰이어도 성공으로 응답한다. */
    @Transactional
    public void unsubscribeOneClick(String unsubscribeToken) {
        Optional<NewsletterSubscription> subscription = subscriptionRepository.findByUnsubscribeToken(unsubscribeToken);
        subscription.ifPresent(this::remove);
    }

    /** 인증 기한이 지난 미인증 구독 신청을 파기한다. */
    @Transactional
    public int deleteExpiredPendingSubscriptions() {
        return subscriptionRepository.deleteByStatusAndVerificationExpiresAtBefore(
            NewsletterSubscriptionStatus.PENDING, LocalDateTime.now());
    }

    private void remove(NewsletterSubscription subscription) {
        boolean wasActive = subscription.getStatus() == NewsletterSubscriptionStatus.ACTIVE;
        String email = subscription.getEmail();
        subscriptionRepository.delete(subscription);
        log.info("Newsletter subscription removed, subscriptionId={}", subscription.getId());

        if (wasActive) {
            mailService.sendUnsubscribedNotice(email, LocalDateTime.now());
        }
    }

    /** Redis 장애로 한도를 확인할 수 없으면 발송하지 않는다(fail-closed). */
    private void requirePermit(String key, int limit, Duration window, String deniedMessage) {
        switch (rateLimiter.acquire(key, limit, window)) {
            case ALLOWED -> {
            }
            case DENIED -> throw new TooManyRequestsException(deniedMessage);
            case UNAVAILABLE -> throw new ServiceUnavailableException("일시적으로 인증 메일을 보낼 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
