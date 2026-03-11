package kr.devport.api.domain.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenCookieService {

    @Value("${app.auth.refresh-cookie-name:devport_refresh_token}")
    private String cookieName;

    @Value("${app.auth.refresh-cookie-path:/api/auth}")
    private String cookiePath;

    @Value("${app.auth.refresh-cookie-domain:}")
    private String cookieDomain;

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean secureCookie;

    @Value("${app.auth.refresh-cookie-same-site:Lax}")
    private String sameSite;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshToken, Duration.ofMillis(refreshTokenExpirationMs)).toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
    }

    public String getCookieName() {
        return cookieName;
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, value)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path(cookiePath)
            .maxAge(maxAge);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        return builder.build();
    }
}
