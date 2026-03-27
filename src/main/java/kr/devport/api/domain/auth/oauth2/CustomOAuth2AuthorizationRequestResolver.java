package kr.devport.api.domain.auth.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * OAuth2 state 파라미터에 Turnstile 토큰, intent, 약관 동의 버전을 실어 보내는 커스텀 resolver.
 * state 형식: originalState~base64Url(JSON{"t":"turnstile","i":"intent","v":"agreedTermsVersion"})
 */
@Slf4j
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private static final String TURNSTILE_COOKIE_NAME = "turnstile_token";
    private static final String STATE_DELIMITER = "~";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

        String intent = request.getParameter("intent");
        String agreedTermsVersion = request.getParameter("agreed_terms_version");

        if (turnstileToken == null || turnstileToken.isEmpty()) {
            log.debug("Turnstile token not found during OAuth2 authorization request");
            return authorizationRequest;
        }

        try {
            ObjectNode payload = OBJECT_MAPPER.createObjectNode();
            payload.put("t", turnstileToken);
            if (intent != null && !intent.isEmpty()) {
                payload.put("i", intent);
            }
            if (agreedTermsVersion != null && !agreedTermsVersion.isEmpty()) {
                payload.put("v", agreedTermsVersion);
            }

            String json = OBJECT_MAPPER.writeValueAsString(payload);
            String encoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));

            String originalState = authorizationRequest.getState();
            String newState = originalState + STATE_DELIMITER + encoded;

            log.debug("Appending OAuth2 state payload (intent={}, hasTermsVersion={})",
                    intent, agreedTermsVersion != null);

            return OAuth2AuthorizationRequest.from(authorizationRequest)
                    .state(newState)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to encode OAuth2 state payload", e);
            return authorizationRequest;
        }
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

    // ── State payload extraction ─────────────────────────────────────

    private static JsonNode decodePayload(String state) {
        if (state == null || !state.contains(STATE_DELIMITER)) {
            return null;
        }
        try {
            String[] parts = state.split("\\" + STATE_DELIMITER, 2);
            if (parts.length < 2) {
                return null;
            }
            String json = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            log.warn("Failed to decode OAuth2 state payload", e);
            return null;
        }
    }

    /** state 파라미터에서 Turnstile 토큰을 복원한다. */
    public static String extractTurnstileTokenFromState(String state) {
        JsonNode node = decodePayload(state);
        return node != null && node.has("t") ? node.get("t").asText() : null;
    }

    /** state 파라미터에서 intent(login/signup)를 복원한다. */
    public static String extractIntentFromState(String state) {
        JsonNode node = decodePayload(state);
        return node != null && node.has("i") ? node.get("i").asText() : null;
    }

    /** state 파라미터에서 약관 동의 버전을 복원한다. */
    public static String extractTermsVersionFromState(String state) {
        JsonNode node = decodePayload(state);
        return node != null && node.has("v") ? node.get("v").asText() : null;
    }
}
