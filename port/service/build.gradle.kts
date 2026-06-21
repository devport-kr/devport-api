// Port use-cases + DTOs. Depends only on ports — no repository-jpa, no web.
dependencies {
    api(project(":port:model"))
    implementation(project(":port:infrastructure"))

    // Cross-domain: resolve comment authors via auth's inbound UserDirectory port (UserSummary).
    implementation(project(":auth:infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-validation")
}
