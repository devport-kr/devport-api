package kr.devport.api.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.devport.api.domain.entity.RefreshToken;
import kr.devport.api.domain.entity.User;
import kr.devport.api.repository.UserRepository;
import kr.devport.api.security.CustomUserDetails;
import kr.devport.api.security.JwtTokenProvider;
import kr.devport.api.service.RefreshTokenService;
import kr.devport.api.service.TurnstileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
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
        // Step 1: Validate Turnstile token from cookie
        String turnstileToken = extractTurnstileTokenFromCookie(request);

        if (turnstileToken == null || turnstileToken.isEmpty()) {
            log.warn("Turnstile token not found in cookies. Redirecting to login with error.");
            return buildFailureRedirectUrl("Turnstile token is missing");
        }

        // Step 2: Validate token with Cloudflare
        String clientIp = getClientIp(request);
        boolean isValid = turnstileService.validateToken(turnstileToken, clientIp);

        if (!isValid) {
            log.warn("Turnstile token validation failed for IP: {}. Redirecting to login with error.", clientIp);
            return buildFailureRedirectUrl("Bot verification failed");
        }

        log.info("Turnstile token validated successfully. Proceeding with OAuth2 login.");

        // Step 3: Generate JWT tokens
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Generate access token (1 hour)
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails.getId());

        // Generate and save refresh token (30 days)
        User user = userRepository.findById(userDetails.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("accessToken", accessToken)
            .queryParam("refreshToken", refreshToken.getToken())
            .build()
            .toUriString();
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
            .filter(cookie -> "turnstile_token".equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets the client's real IP address (handles proxy headers)
     */
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
        // If multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Builds failure redirect URL with error message
     */
    private String buildFailureRedirectUrl(String errorMessage) {
        return UriComponentsBuilder.fromUriString(failureRedirectUri)
            .queryParam("error", errorMessage)
            .build()
            .toUriString();
    }
}
