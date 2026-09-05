// Shared security primitives: CustomUserDetails (principal) + JwtTokenProvider. No domain deps.
dependencies {
    api("org.springframework.security:spring-security-core")
    api("org.springframework.security:spring-security-oauth2-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
