package kr.devport.api.domain.newsletter.service;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.exception.DuplicateEmailException;
import kr.devport.api.domain.common.exception.EmailDeliveryException;
import kr.devport.api.domain.common.exception.TokenExpiredException;
import kr.devport.api.domain.common.exception.TokenNotFoundException;
import kr.devport.api.domain.common.exception.TooManyRequestsException;
import kr.devport.api.domain.common.ratelimit.RedisRateLimiter;
import kr.devport.api.domain.newsletter.dto.request.NewsletterSubscribeRequest;
import kr.devport.api.domain.newsletter.dto.response.NewsletterActionResponse;
import kr.devport.api.domain.newsletter.dto.response.NewsletterSubscriptionResponse;
import kr.devport.api.domain.newsletter.entity.NewsletterSubscription;
import kr.devport.api.domain.newsletter.enums.NewsletterSubscriptionStatus;
import kr.devport.api.domain.newsletter.repository.NewsletterSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NewsletterSubscriptionServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private NewsletterSubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NewsletterMailService mailService;

    @Mock
    private RedisRateLimiter rateLimiter;

    private NewsletterSubscriptionService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new NewsletterSubscriptionService(subscriptionRepository, userRepository, mailService, rateLimiter);
        user = User.builder().id(USER_ID).username("tester").build();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(subscriptionRepository.save(any(NewsletterSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private NewsletterSubscribeRequest subscribeRequest(String email) {
        return NewsletterSubscribeRequest.builder().email(email).agreed(true).build();
    }

    private NewsletterSubscription subscription(NewsletterSubscriptionStatus status, String email) {
        LocalDateTime now = LocalDateTime.now();
        return NewsletterSubscription.builder()
            .id(11L)
            .user(user)
            .email(email)
            .status(status)
            .unsubscribeToken("unsubscribe-token")
            .consentedAt(now.minusDays(1))
            .createdAt(now.minusDays(1))
            .updatedAt(now.minusDays(1))
            .build();
    }

    @Test
    void subscribeCreatesPendingSubscriptionAndSendsHashedTokenLink() {
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        NewsletterSubscriptionResponse response = service.subscribe(USER_ID, subscribeRequest("  Tester@Example.COM "));

        ArgumentCaptor<NewsletterSubscription> saved = ArgumentCaptor.forClass(NewsletterSubscription.class);
        verify(subscriptionRepository).save(saved.capture());
        ArgumentCaptor<String> rawToken = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendVerificationEmail(eq("tester@example.com"), rawToken.capture());

        NewsletterSubscription subscription = saved.getValue();
        assertThat(subscription.getStatus()).isEqualTo(NewsletterSubscriptionStatus.PENDING);
        assertThat(subscription.getEmail()).isEqualTo("tester@example.com");
        assertThat(subscription.getUser()).isSameAs(user);
        assertThat(subscription.getConsentedAt()).isNotNull();
        assertThat(subscription.getUnsubscribeToken()).hasSize(43);
        assertThat(subscription.getVerificationTokenHash())
            .isEqualTo(NewsletterTokens.sha256(rawToken.getValue()))
            .isNotEqualTo(rawToken.getValue());
        assertThat(subscription.getVerificationExpiresAt()).isAfter(LocalDateTime.now().plusHours(23));

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getVerificationExpiresAt()).isNotNull();
    }

    @Test
    void subscribeIsNoOpWhenAlreadyActiveWithSameEmail() {
        when(subscriptionRepository.findByUserId(USER_ID))
            .thenReturn(Optional.of(subscription(NewsletterSubscriptionStatus.ACTIVE, "tester@example.com")));

        NewsletterSubscriptionResponse response = service.subscribe(USER_ID, subscribeRequest("TESTER@example.com"));

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(mailService, never()).sendVerificationEmail(anyString(), anyString());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribeRejectsEmailActiveOnAnotherAccount() {
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(subscriptionRepository.existsByEmailAndStatusAndUserIdNot(
            "taken@example.com", NewsletterSubscriptionStatus.ACTIVE, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.subscribe(USER_ID, subscribeRequest("taken@example.com")))
            .isInstanceOf(DuplicateEmailException.class);
        verify(mailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void subscribeEnforcesResendCooldown() {
        NewsletterSubscription pending = subscription(NewsletterSubscriptionStatus.PENDING, "tester@example.com");
        pending.setVerificationSentAt(LocalDateTime.now().minusSeconds(10));
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.subscribe(USER_ID, subscribeRequest("tester@example.com")))
            .isInstanceOf(TooManyRequestsException.class);
        verify(mailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void subscribeEnforcesHourlyLimit() {
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(rateLimiter.tryAcquire(eq("newsletter:verify:user:" + USER_ID), anyInt(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.subscribe(USER_ID, subscribeRequest("tester@example.com")))
            .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void changingEmailOfActiveSubscriptionRequiresVerificationAgain() {
        NewsletterSubscription active = subscription(NewsletterSubscriptionStatus.ACTIVE, "old@example.com");
        active.setVerifiedAt(LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(active));

        service.subscribe(USER_ID, subscribeRequest("new@example.com"));

        assertThat(active.getStatus()).isEqualTo(NewsletterSubscriptionStatus.PENDING);
        assertThat(active.getEmail()).isEqualTo("new@example.com");
        assertThat(active.getVerifiedAt()).isNull();
        assertThat(active.getUnsubscribeToken()).isEqualTo("unsubscribe-token");
        verify(mailService).sendVerificationEmail(eq("new@example.com"), anyString());
    }

    @Test
    void subscribePropagatesMailFailure() {
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        doThrow(new EmailDeliveryException("fail", new RuntimeException()))
            .when(mailService).sendVerificationEmail(anyString(), anyString());

        assertThatThrownBy(() -> service.subscribe(USER_ID, subscribeRequest("tester@example.com")))
            .isInstanceOf(EmailDeliveryException.class);
    }

    @Test
    void confirmActivatesPendingSubscription() {
        NewsletterSubscription pending = subscription(NewsletterSubscriptionStatus.PENDING, "tester@example.com");
        pending.setVerificationTokenHash(NewsletterTokens.sha256("raw-token"));
        pending.setVerificationExpiresAt(LocalDateTime.now().plusHours(1));
        when(subscriptionRepository.findByVerificationTokenHash(NewsletterTokens.sha256("raw-token")))
            .thenReturn(Optional.of(pending));

        NewsletterActionResponse response = service.confirm("raw-token");

        assertThat(pending.getStatus()).isEqualTo(NewsletterSubscriptionStatus.ACTIVE);
        assertThat(pending.getVerifiedAt()).isNotNull();
        assertThat(pending.getVerificationTokenHash()).isNull();
        assertThat(pending.getVerificationExpiresAt()).isNull();
        assertThat(response.getEmail()).isEqualTo("te***@example.com");
        verify(mailService).sendSubscribedNotice(pending);
    }

    @Test
    void confirmRejectsExpiredLink() {
        NewsletterSubscription pending = subscription(NewsletterSubscriptionStatus.PENDING, "tester@example.com");
        pending.setVerificationExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(subscriptionRepository.findByVerificationTokenHash(anyString())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.confirm("raw-token")).isInstanceOf(TokenExpiredException.class);
        assertThat(pending.getStatus()).isEqualTo(NewsletterSubscriptionStatus.PENDING);
    }

    @Test
    void confirmRejectsUnknownToken() {
        when(subscriptionRepository.findByVerificationTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm("nope")).isInstanceOf(TokenNotFoundException.class);
    }

    @Test
    void confirmRejectsEmailActivatedByAnotherAccountMeanwhile() {
        NewsletterSubscription pending = subscription(NewsletterSubscriptionStatus.PENDING, "tester@example.com");
        pending.setVerificationExpiresAt(LocalDateTime.now().plusHours(1));
        when(subscriptionRepository.findByVerificationTokenHash(anyString())).thenReturn(Optional.of(pending));
        when(subscriptionRepository.existsByEmailAndStatusAndUserIdNot(
            "tester@example.com", NewsletterSubscriptionStatus.ACTIVE, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.confirm("raw-token")).isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void unsubscribeByTokenDeletesActiveSubscriptionAndNotifies() {
        NewsletterSubscription active = subscription(NewsletterSubscriptionStatus.ACTIVE, "tester@example.com");
        when(subscriptionRepository.findByUnsubscribeToken("unsubscribe-token")).thenReturn(Optional.of(active));

        service.unsubscribeByToken("unsubscribe-token");

        verify(subscriptionRepository).delete(active);
        verify(mailService).sendUnsubscribedNotice(eq("tester@example.com"), any(LocalDateTime.class));
    }

    @Test
    void unsubscribingPendingSubscriptionDoesNotSendNotice() {
        NewsletterSubscription pending = subscription(NewsletterSubscriptionStatus.PENDING, "tester@example.com");
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(pending));

        service.unsubscribe(USER_ID);

        verify(subscriptionRepository).delete(pending);
        verify(mailService, never()).sendUnsubscribedNotice(anyString(), any());
    }

    @Test
    void unsubscribeByUnknownTokenFailsButOneClickIsIdempotent() {
        when(subscriptionRepository.findByUnsubscribeToken("gone")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unsubscribeByToken("gone")).isInstanceOf(TokenNotFoundException.class);
        service.unsubscribeOneClick("gone");
        verify(subscriptionRepository, never()).delete(any());
    }

    @Test
    void getMySubscriptionReturnsNoneWhenAbsent() {
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThat(service.getMySubscription(USER_ID).getStatus()).isEqualTo("NONE");
    }
}
