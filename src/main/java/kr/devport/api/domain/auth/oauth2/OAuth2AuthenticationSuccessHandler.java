package kr.devport.api.domain.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.common.security.CustomUserDetails;
import kr.devport.api.domain.auth.service.OAuth2ExchangeCodeService;
import kr.devport.api.domain.auth.service.TurnstileService;
import kr.devport.api.domain.common.logging.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2ExchangeCodeService oAuth2ExchangeCodeService;
    private final UserRepository userRepository;
    private final TurnstileService turnstileService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Value("${app.oauth2.failure-redirect-uri}")
    private String failureRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        String state = request.getParameter("state");
        String turnstileToken = CustomOAuth2AuthorizationRequestResolver.extractTurnstileTokenFromState(state);

        if (turnstileToken == null || turnstileToken.isEmpty()) {
            log.debug("Missing Turnstile token in OAuth2 state");
            return buildFailureRedirectUrl("Turnstile token is missing");
        }

        String clientIp = getClientIp(request);
        boolean isValid = turnstileService.validateToken(turnstileToken, clientIp);

        if (!isValid) {
            log.warn("Turnstile validation failed for OAuth2 login, clientIp={}", LogSanitizer.maskIp(clientIp));
            return buildFailureRedirectUrl("Bot verification failed");
        }

        log.debug("Turnstile validation succeeded for OAuth2 login");

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        String exchangeCode = oAuth2ExchangeCodeService.createExchangeCode(user, request);

        return UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("code", exchangeCode)
            .build()
            .toUriString();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 여러 IP가 전달되면 첫 번째 값을 사용한다.
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String buildFailureRedirectUrl(String errorMessage) {
        return UriComponentsBuilder.fromUriString(failureRedirectUri)
            .queryParam("error", errorMessage)
            .build()
            .toUriString();
    }
}
