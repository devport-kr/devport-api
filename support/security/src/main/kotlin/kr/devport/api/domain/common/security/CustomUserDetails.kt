package kr.devport.api.domain.common.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * Authenticated principal. A plain holder with no domain dependency — the auth module builds it
 * from its `User` entity via a factory, keeping this shared type free of domain coupling.
 */
class CustomUserDetails(
    val id: Long,
    val email: String?,
    private val usernameValue: String?,
    private val passwordValue: String?,
    private val nameValue: String?,
    private val authoritiesValue: Collection<out GrantedAuthority>?,
    private val attributesValue: Map<String, Any>?,
) : UserDetails,
    OAuth2User {
    fun withAttributes(attributes: Map<String, Any>?): CustomUserDetails =
        CustomUserDetails(id, email, usernameValue, passwordValue, nameValue, authoritiesValue, attributes)

    override fun getUsername(): String = usernameValue ?: ""

    override fun getPassword(): String? = passwordValue

    override fun getName(): String = nameValue ?: ""

    override fun getAuthorities(): Collection<out GrantedAuthority> = authoritiesValue ?: emptyList()

    override fun getAttributes(): Map<String, Any> = attributesValue ?: emptyMap()

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
