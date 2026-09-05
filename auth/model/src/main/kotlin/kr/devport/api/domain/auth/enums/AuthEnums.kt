package kr.devport.api.domain.auth.enums

// Entry names are intentionally lowercase: they map 1:1 to OAuth2 registrationIds and the
// persisted `auth_provider` STRING column, so they must not be renamed.
@Suppress("ktlint:standard:enum-entry-name-case")
enum class AuthProvider {
    github,
    google,
    naver,
    local,
}

enum class UserRole {
    USER,
    ADMIN,
}
