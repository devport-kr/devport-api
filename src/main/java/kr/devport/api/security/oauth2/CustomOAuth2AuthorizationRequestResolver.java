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

/**
 * Custom OAuth2 authorization request resolver that appends Turnstile token to the state parameter
 * This allows us to pass the Turnstile token through the OAuth2 flow and validate it after callback
 */
@Slf4j
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private static final String TURNSTILE_COOKIE_NAME = "turnstile_token";
    private static final String STATE_DELIMITER = "|";

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

    /**
     * Customizes the authorization request by appending Turnstile token to state parameter
     */
    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request) {

        if (authorizationRequest == null) {
            return null;
        }

        // Try to extract Turnstile token from query parameter first, then cookie
        String turnstileToken = request.getParameter("turnstile_token");

        if (turnstileToken == null || turnstileToken.isEmpty()) {
            // Fallback to cookie
            turnstileToken = extractTurnstileTokenFromCookie(request);
        }

        if (turnstileToken == null || turnstileToken.isEmpty()) {
            log.warn("Turnstile token not found in query parameter or cookies during OAuth2 authorization request");
            // Continue without Turnstile token - will be validated later
            return authorizationRequest;
        }

        // Append Turnstile token to state parameter
        // Format: originalState|base64(turnstileToken)
        String originalState = authorizationRequest.getState();
        String encodedTurnstileToken = Base64.getEncoder().encodeToString(turnstileToken.getBytes());
        String newState = originalState + STATE_DELIMITER + encodedTurnstileToken;

        log.debug("Appending Turnstile token to OAuth2 state parameter");

        // Build new authorization request with modified state
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .state(newState)
                .build();
    }

    /**
     * Extracts Turnstile token from request cookies
     */
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

    /**
     * Utility method to extract Turnstile token from state parameter
     * Called by OAuth2AuthenticationSuccessHandler after callback
     */
    public static String extractTurnstileTokenFromState(String state) {
        if (state == null || !state.contains(STATE_DELIMITER)) {
            return null;
        }

        try {
            String[] parts = state.split("\\" + STATE_DELIMITER);
            if (parts.length < 2) {
                return null;
            }

            // Decode base64 Turnstile token
            String encodedToken = parts[1];
            byte[] decodedBytes = Base64.getDecoder().decode(encodedToken);
            return new String(decodedBytes);

        } catch (Exception e) {
            log.error("Failed to extract Turnstile token from state parameter", e);
            return null;
        }
    }
}
