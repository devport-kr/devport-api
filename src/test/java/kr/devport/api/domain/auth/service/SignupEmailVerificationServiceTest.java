package kr.devport.api.domain.auth.service;

import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.exception.BotVerificationException;
import kr.devport.api.domain.common.exception.DuplicateEmailException;
import kr.devport.api.domain.common.exception.EmailDeliveryException;
import kr.devport.api.domain.common.exception.EmailVerificationCodeException;
import kr.devport.api.domain.common.exception.TooManyRequestsException;
import kr.devport.api.domain.common.ratelimit.RedisRateLimiter;
import kr.devport.api.support.InMemoryRedis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignupEmailVerificationServiceTest {

    private static final String CLIENT_IP = "203.0.113.7";
    private static final String EMAIL = "kim@example.com";

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private TurnstileService turnstileService;

    @Mock
    private RedisRateLimiter rateLimiter;

    private Map<String, Object> redis;
    private SignupEmailVerificationService service;

    @BeforeEach
    void setUp() {
        redis = InMemoryRedis.backing(redisTemplate);
        service = new SignupEmailVerificationService(redisTemplate, userRepository, emailService, turnstileService, rateLimiter);
        ReflectionTestUtils.setField(service, "globalLimitPerHour", 200);
        when(turnstileService.validateToken("turnstile", CLIENT_IP)).thenReturn(true);
        when(rateLimiter.acquire(anyString(), anyInt(), any())).thenReturn(RedisRateLimiter.Result.ALLOWED);
    }

    private String sendAndCaptureCode(String email) {
        service.sendCode(email, "turnstile", CLIENT_IP);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendSignupCode(eq(SignupEmailVerificationService.normalizeEmail(email)), code.capture(), eq(10L));
        return code.getValue();
    }

    @Test
    void correctCodeYieldsSingleUseTokenBoundToEmail() {
        String code = sendAndCaptureCode(" Kim@Example.COM ");

        assertThat(code).matches("\\d{6}");
        assertThat(redis.values()).doesNotContain(code);

        String token = service.verifyCode("kim@example.com", code);

        assertThatCode(() -> service.requireVerified(EMAIL, token)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.requireVerified("other@example.com", token))
            .isInstanceOf(EmailVerificationCodeException.class);

        service.consume(token);
        assertThatThrownBy(() -> service.requireVerified(EMAIL, token))
            .isInstanceOf(EmailVerificationCodeException.class);
    }

    @Test
    void codeCannotBeReusedAfterVerification() {
        String code = sendAndCaptureCode(EMAIL);
        service.verifyCode(EMAIL, code);

        assertThatThrownBy(() -> service.verifyCode(EMAIL, code))
            .isInstanceOf(EmailVerificationCodeException.class)
            .hasMessageContaining("만료");
    }

    @Test
    void wrongCodesCountDownAndExhaustTheCode() {
        String code = sendAndCaptureCode(EMAIL);
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int remaining = SignupEmailVerificationService.MAX_ATTEMPTS - 1; remaining > 0; remaining--) {
            assertThatThrownBy(() -> service.verifyCode(EMAIL, wrong))
                .isInstanceOf(EmailVerificationCodeException.class)
                .hasMessageContaining("남은 시도 " + remaining + "회");
        }
        assertThatThrownBy(() -> service.verifyCode(EMAIL, wrong))
            .isInstanceOf(EmailVerificationCodeException.class)
            .hasMessageContaining("너무 많이 틀렸습니다");

        // 시도를 다 쓰면 맞는 번호도 더는 통하지 않는다
        assertThatThrownBy(() -> service.verifyCode(EMAIL, code))
            .isInstanceOf(EmailVerificationCodeException.class)
            .hasMessageContaining("만료");
    }

    @Test
    void resendingIssuesANewCodeAndResetsAttempts() {
        String first = sendAndCaptureCode(EMAIL);
        String wrong = first.equals("000000") ? "111111" : "000000";
        assertThatThrownBy(() -> service.verifyCode(EMAIL, wrong)).isInstanceOf(EmailVerificationCodeException.class);
        redis.keySet().removeIf(key -> key.contains(":cooldown:")); // 1분 경과

        clearInvocations(emailService);
        String second = sendAndCaptureCode(EMAIL);

        assertThat(redis.keySet()).noneMatch(key -> key.contains(":attempts:"));
        if (!first.equals(second)) {
            assertThatThrownBy(() -> service.verifyCode(EMAIL, first)).isInstanceOf(EmailVerificationCodeException.class);
        }
        assertThat(service.verifyCode(EMAIL, second)).isNotBlank();
    }

    @Test
    void registeredEmailIsRejectedBeforeBotCheck() {
        when(userRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.sendCode("KIM@example.com", "turnstile", CLIENT_IP))
            .isInstanceOf(DuplicateEmailException.class);
        verify(turnstileService, never()).validateToken(anyString(), anyString());
        verify(emailService, never()).sendSignupCode(anyString(), anyString(), anyLong());
    }

    @Test
    void resendWithinCooldownIsRejected() {
        sendAndCaptureCode(EMAIL);

        assertThatThrownBy(() -> service.sendCode(EMAIL, "turnstile", CLIENT_IP))
            .isInstanceOf(TooManyRequestsException.class)
            .hasMessageContaining("1분");
    }

    @Test
    void failedBotCheckSendsNothing() {
        when(turnstileService.validateToken("turnstile", CLIENT_IP)).thenReturn(false);

        assertThatThrownBy(() -> service.sendCode(EMAIL, "turnstile", CLIENT_IP))
            .isInstanceOf(BotVerificationException.class);
        verify(rateLimiter, never()).acquire(anyString(), anyInt(), any());
        verify(emailService, never()).sendSignupCode(anyString(), anyString(), anyLong());
    }

    @Test
    void rateLimitDenialReleasesEarlierPermits() {
        when(rateLimiter.acquire(eq("signup-code:global"), anyInt(), any())).thenReturn(RedisRateLimiter.Result.DENIED);

        assertThatThrownBy(() -> service.sendCode(EMAIL, "turnstile", CLIENT_IP))
            .isInstanceOf(TooManyRequestsException.class);
        verify(rateLimiter).release("signup-code:ip:" + CLIENT_IP);
        verify(rateLimiter).release("signup-code:global");
        verify(emailService, never()).sendSignupCode(anyString(), anyString(), anyLong());
    }

    @Test
    void mailFailureReleasesPermitsAndAllowsImmediateRetry() {
        doThrow(new EmailDeliveryException("smtp down", new RuntimeException()))
            .when(emailService).sendSignupCode(anyString(), anyString(), anyLong());

        assertThatThrownBy(() -> service.sendCode(EMAIL, "turnstile", CLIENT_IP))
            .isInstanceOf(EmailDeliveryException.class);
        verify(rateLimiter).release("signup-code:ip:" + CLIENT_IP);
        verify(rateLimiter).release("signup-code:global");
        assertThat(redis.keySet()).noneMatch(key -> key.contains(":cooldown:"));
    }

    @Test
    void privateProxyIpSkipsIpLimit() {
        when(turnstileService.validateToken("turnstile", "10.0.1.179")).thenReturn(true);

        service.sendCode(EMAIL, "turnstile", "10.0.1.179");

        verify(rateLimiter, never()).acquire(eq("signup-code:ip:10.0.1.179"), anyInt(), any());
    }

    @Test
    void missingCodeIsReportedAsExpired() {
        assertThatThrownBy(() -> service.verifyCode(EMAIL, "123456"))
            .isInstanceOf(EmailVerificationCodeException.class)
            .hasMessageContaining("만료");
    }
}
