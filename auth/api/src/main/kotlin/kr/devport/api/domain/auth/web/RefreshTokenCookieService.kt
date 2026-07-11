package kr.devport.api.domain.auth.web

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration

/** Web adapter: writes/clears the refresh-token cookie. Servlet-coupled, so it lives in the api layer. */
@Service
class RefreshTokenCookieService {
    @Value("\${app.auth.refresh-cookie-name:devport_refresh_token}")
    private lateinit var cookieName: String

    @Value("\${app.auth.refresh-cookie-path:/api/auth}")
    private lateinit var cookiePath: String

    @Value("\${app.auth.refresh-cookie-domain:}")
    private lateinit var cookieDomain: String

    @Value("\${app.auth.refresh-cookie-secure:false}")
    private var secureCookie: Boolean = false

    @Value("\${app.auth.refresh-cookie-same-site:Lax}")
    private lateinit var sameSite: String

    @Value("\${app.jwt.refresh-token-expiration-ms}")
    private var refreshTokenExpirationMs: Long = 0

    fun addRefreshTokenCookie(
        response: HttpServletResponse,
        refreshToken: String,
    ) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(refreshToken, Duration.ofMillis(refreshTokenExpirationMs)).toString())
    }

    fun clearRefreshTokenCookie(response: HttpServletResponse) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString())
    }

    fun getCookieName(): String = cookieName

    private fun buildCookie(
        value: String,
        maxAge: Duration,
    ): ResponseCookie {
        val builder =
            ResponseCookie
                .from(cookieName, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path(cookiePath)
                .maxAge(maxAge)
        if (cookieDomain.isNotBlank()) builder.domain(cookieDomain)
        return builder.build()
    }
}
