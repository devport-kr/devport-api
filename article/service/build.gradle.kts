// Article use-cases + all DTOs (Kotlin). Depends only on ports — no repository-jpa, no OpenAI.
dependencies {
    api(project(":article:model"))
    implementation(project(":article:infrastructure"))

    // Cross-domain: resolve comment authors via auth's inbound UserDirectory port (UserSummary).
    implementation(project(":auth:infrastructure"))

    implementation(project(":support:cache"))
    implementation(project(":support:exception"))

    implementation("org.springframework.boot:spring-boot-starter-validation")
}
