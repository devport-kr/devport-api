package kr.devport.api.integration;

import com.jayway.jsonpath.JsonPath;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.enums.UserRole;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.auth.service.TurnstileService;
import kr.devport.api.domain.common.ratelimit.RedisRateLimiter;
import kr.devport.api.domain.common.security.JwtTokenProvider;
import kr.devport.api.domain.newsletter.entity.NewsletterIssue;
import kr.devport.api.domain.newsletter.enums.NewsletterIssueStatus;
import kr.devport.api.domain.newsletter.repository.NewsletterIssueRepository;
import kr.devport.api.domain.newsletter.repository.NewsletterSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityFilterChain + JPA(H2) 위에서 아이디 가입 → 뉴스레터 double opt-in → 관리자 발송 → one-click 해지 흐름을 검증한다.
 * 외부 연동(Turnstile, SMTP, Redis rate limit)만 mock 한다.
 */
@SpringBootTest(properties = {
    "app.jwt.secret=integration-test-secret-key-that-is-long-enough-for-hmac-sha512-0123456789abcdef",
    "app.newsletter.send-batch-interval-ms=0",
    // JavaMailSender를 mock으로 대체하므로 JavaMailSenderImpl 기반 mail health indicator는 끈다
    "management.health.mail.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignupAndNewsletterFlowIntegrationTest {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NewsletterSubscriptionRepository subscriptionRepository;

    @Autowired
    private NewsletterIssueRepository issueRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private TurnstileService turnstileService;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        when(turnstileService.validateToken(anyString(), any())).thenReturn(true);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any())).thenReturn(true);
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private static String signupJson(String username) {
        return """
            {"username":"%s","password":"Password@123","agreedTermsVersion":"2026-03-24","turnstileToken":"turnstile"}
            """.formatted(username);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void localSignupNewsletterOptInAdminSendAndOneClickUnsubscribe() throws Exception {
        // 1. 아이디/비밀번호만으로 가입 → 즉시 로그인 (access token + refresh cookie)
        MvcResult signup = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("flowuser")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("devport_refresh_token=")))
            .andReturn();
        String accessToken = JsonPath.read(signup.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupJson("FlowUser")))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/auth/check-username").param("username", "flowuser"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.available").value(false));

        // 2. 이메일 인증 없이 로그인 가능
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"flowuser\",\"password\":\"Password@123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("flowuser"))
            .andExpect(jsonPath("$.email").value(nullValue()));

        // 3. 뉴스레터 구독 신청 → PENDING + 인증 메일
        mockMvc.perform(get("/api/newsletter/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NONE"));

        mockMvc.perform(post("/api/newsletter/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@example.com\",\"agreed\":false}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/newsletter/me")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"Flow@Example.com\",\"agreed\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.email").value("flow@example.com"));

        ArgumentCaptor<MimeMessage> mailCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(mailCaptor.capture());
        MimeMessage verificationMail = mailCaptor.getValue();
        assertThat(verificationMail.getAllRecipients()[0].toString()).isEqualTo("flow@example.com");
        Matcher tokenMatcher = TOKEN_IN_LINK.matcher((String) verificationMail.getContent());
        assertThat(tokenMatcher.find()).isTrue();
        String verificationToken = tokenMatcher.group(1);

        // 4. 메일 링크로 확인 (로그인 불필요) → ACTIVE, 같은 토큰 재사용 불가
        mockMvc.perform(post("/api/newsletter/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + verificationToken + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("fl***@example.com"));

        mockMvc.perform(post("/api/newsletter/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + verificationToken + "\"}"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/newsletter/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.verifiedAt").isNotEmpty());

        // 5. 관리자만 발송 가능, 발송은 커밋 후 비동기
        mockMvc.perform(post("/api/admin/newsletter/issues")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"주간 소식\",\"content\":\"안녕하세요 https://devport.kr\"}"))
            .andExpect(status().isForbidden());

        LocalDateTime now = LocalDateTime.now();
        User admin = userRepository.save(User.builder()
            .username("flowadmin")
            .password("unused")
            .name("flowadmin")
            .authProvider(AuthProvider.local)
            .role(UserRole.ADMIN)
            .createdAt(now)
            .updatedAt(now)
            .build());
        String adminToken = jwtTokenProvider.createAccessToken(admin.getId());

        mockMvc.perform(get("/api/admin/newsletter/stats").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeCount").value(1));

        MvcResult created = mockMvc.perform(post("/api/admin/newsletter/issues")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"주간 소식\",\"content\":\"안녕하세요 https://devport.kr\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("SENDING"))
            .andExpect(jsonPath("$.recipientCount").value(1))
            .andReturn();
        long issueId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

        NewsletterIssue issue = awaitDispatch(issueId);
        assertThat(issue.getStatus()).isEqualTo(NewsletterIssueStatus.SENT);
        assertThat(issue.getSentCount()).isEqualTo(1);

        Long userId = userRepository.findByUsername("flowuser").orElseThrow().getId();
        String unsubscribeToken = subscriptionRepository.findByUserId(userId).orElseThrow().getUnsubscribeToken();
        ArgumentCaptor<MimeMessage[]> batchCaptor = ArgumentCaptor.forClass(MimeMessage[].class);
        verify(mailSender).send(batchCaptor.capture());
        MimeMessage newsletter = batchCaptor.getValue()[0];
        assertThat(newsletter.getHeader("List-Unsubscribe")[0]).contains("token=" + unsubscribeToken);

        // 6. 메일 클라이언트 one-click 해지 (로그인 불필요) → 구독 정보 삭제
        mockMvc.perform(get("/api/newsletter/unsubscribe/one-click").param("token", unsubscribeToken))
            .andExpect(status().isFound())
            .andExpect(header().string(HttpHeaders.LOCATION, containsString("/newsletter/unsubscribe?token=" + unsubscribeToken)));

        mockMvc.perform(post("/api/newsletter/unsubscribe/one-click")
                .param("token", unsubscribeToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("List-Unsubscribe=One-Click"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/newsletter/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(jsonPath("$.status").value("NONE"));
        assertThat(subscriptionRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void signupValidatesTermsAndBotTokenAndNewsletterRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"staleterms\",\"password\":\"Password@123\",\"agreedTermsVersion\":\"2020-01-01\",\"turnstileToken\":\"t\"}"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nobot\",\"password\":\"Password@123\",\"agreedTermsVersion\":\"2026-03-24\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors.turnstileToken").exists());

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"weakpw\",\"password\":\"password123\",\"agreedTermsVersion\":\"2026-03-24\",\"turnstileToken\":\"t\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors.password").exists());

        mockMvc.perform(get("/api/newsletter/me")).andExpect(status().isUnauthorized());
    }

    private NewsletterIssue awaitDispatch(long issueId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            NewsletterIssue issue = issueRepository.findById(issueId).orElseThrow();
            if (issue.getStatus() != NewsletterIssueStatus.SENDING) {
                return issue;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Newsletter dispatch did not finish in time");
    }
}
