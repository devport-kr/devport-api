package kr.devport.api.domain.auth.oauth2;

import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.enums.AuthProvider;
import kr.devport.api.domain.auth.enums.UserRole;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.auth.service.OAuth2ExchangeCodeService;
import kr.devport.api.domain.auth.service.TurnstileService;
import kr.devport.api.domain.common.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private OAuth2ExchangeCodeService oAuth2ExchangeCodeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TurnstileService turnstileService;

    private OAuth2AuthenticationSuccessHandler successHandler;

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2AuthenticationSuccessHandler(
            oAuth2ExchangeCodeService,
            userRepository,
            turnstileService
        );
        ReflectionTestUtils.setField(successHandler, "redirectUri", "https://devport.kr/oauth2/redirect");
        ReflectionTestUtils.setField(successHandler, "failureRedirectUri", "https://devport.kr/login");
    }

    @Test
    void determineTargetUrlUsesOneTimeCodeInsteadOfBearerTokens() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("127.0.0.1");
        request.setParameter("state", buildState("turnstile-token"));

        User user = User.builder()
            .id(1L)
            .email("oauth@devport.kr")
            .username("oauth-user")
            .authProvider(AuthProvider.google)
            .role(UserRole.USER)
            .build();
        CustomUserDetails userDetails = CustomUserDetails.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(turnstileService.validateToken("turnstile-token", "127.0.0.1")).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(oAuth2ExchangeCodeService.createExchangeCode(user, request)).thenReturn("one-time-code");

        String targetUrl = successHandler.determineTargetUrl(request, response, authentication);

        assertTrue(targetUrl.contains("code=one-time-code"));
        assertFalse(targetUrl.contains("accessToken"));
        assertFalse(targetUrl.contains("refreshToken"));
    }

    private String buildState(String turnstileToken) {
        String encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(turnstileToken.getBytes(StandardCharsets.UTF_8));
        return "oauth-state~" + encoded;
    }
}
