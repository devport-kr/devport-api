package kr.devport.api.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Arrays;
import java.util.Base64;

/** OAuth2 state 파라미터에 Turnstile 토큰을 실어 보내는 커스텀 resolver. */
@Slf4j
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private static final String TURNSTILE_COOKIE_NAME = "turnstile_token";
    private static final String STATE_DELIMITER = "~";

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request) {

        if (authorizationRequest == null) {
            return null;
        }

        String turnstileToken = request.getParameter("turnstile_token");

        if (turnstileToken == null || turnstileToken.isEmpty()) {
            turnstileToken = extractTurnstileTokenFromCookie(request);
        }

        if (turnstileToken == null || turnstileToken.isEmpty()) {
            log.warn("Turnstile token not found in query parameter or cookies during OAuth2 authorization request");
            return authorizationRequest;
        }

        // state 형식: originalState~base64Url(turnstileToken)
        // URL-safe Base64 encoding과 tilde 구분자를 사용하여 URL에서 문제가 되는 문자 방지
        String originalState = authorizationRequest.getState();
        String encodedTurnstileToken = Base64.getUrlEncoder().withoutPadding().encodeToString(turnstileToken.getBytes());
        String newState = originalState + STATE_DELIMITER + encodedTurnstileToken;

        log.debug("Appending Turnstile token to OAuth2 state parameter");

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .state(newState)
                .build();
    }

    private String extractTurnstileTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> TURNSTILE_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /** state 파라미터에 인코딩된 Turnstile 토큰을 복원한다. */
    public static String extractTurnstileTokenFromState(String state) {
        if (state == null || !state.contains(STATE_DELIMITER)) {
            return null;
        }

        try {
            String[] parts = state.split("\\" + STATE_DELIMITER);
            if (parts.length < 2) {
                return null;
            }

            String encodedToken = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedToken);
            return new String(decodedBytes);

        } catch (Exception e) {
            log.error("Failed to extract Turnstile token from state parameter", e);
            return null;
        }
    }
}
