package kr.devport.api.domain.auth.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.repository.UserRepository
import kr.devport.api.domain.common.security.CustomUserDetails
import kr.devport.api.domain.common.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

/** Builds the shared CustomUserDetails principal from the auth User entity. */
object CustomUserDetailsFactory {
    fun create(user: User): CustomUserDetails =
        CustomUserDetails(
            requireNotNull(user.id) { "Authenticated user has no id" },
            user.email,
            user.username ?: user.email,
            user.password,
            user.name,
            listOf(SimpleGrantedAuthority("ROLE_" + user.role.name)),
            null,
        )
}

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails {
        val user =
            userRepository
                .findByUsername(username)
                .orElseThrow { UsernameNotFoundException("User not found with username: $username") }
        return CustomUserDetailsFactory.create(user)
    }
}

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val jwt = getJwtFromRequest(request)
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                val userId = jwtTokenProvider.getUserIdFromToken(jwt)
                val user =
                    userRepository.findById(userId).orElseThrow { RuntimeException("User not found with id: $userId") }
                val userDetails = CustomUserDetailsFactory.create(user)
                val authentication =
                    UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
                authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (ex: Exception) {
            log.debug("JWT authentication skipped: {}", ex.message)
        }
        filterChain.doFilter(request, response)
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
