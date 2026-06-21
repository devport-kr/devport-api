package kr.devport.api.domain.auth

import kr.devport.api.domain.auth.enums.UserRole

/**
 * Cross-domain view of a user — the only user shape other domains may consume (via [UserDirectory]).
 * Pure domain model: no JPA, no web. Other domains reference users by id and resolve to this.
 */
data class UserSummary(
    val id: Long,
    val username: String?,
    val name: String?,
    val email: String?,
    val profileImageUrl: String?,
    val flair: String?,
    val flairColor: String?,
    val role: UserRole,
)
