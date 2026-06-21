// Auth use-cases, email, DTOs. Depends only on ports (:auth:infrastructure) — no web/redis/oauth2.
dependencies {
    api(project(":auth:model"))
    implementation(project(":auth:infrastructure"))
    implementation(project(":support:security"))
    implementation(project(":support:exception"))
    implementation(project(":support:logging"))

    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.security:spring-security-crypto")
}
