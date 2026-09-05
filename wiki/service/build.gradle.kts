// Wiki use-cases, DTOs, and RAG orchestration (Kotlin). Depends only on ports —
// no repository-jpa, no OpenAI, no Redis, no auth, no web.
dependencies {
    api(project(":wiki:model"))
    implementation(project(":wiki:infrastructure"))

    // Cross-domain: resolve projects via port's inbound ProjectDirectory port (ProjectView).
    implementation(project(":port:infrastructure"))

    implementation(project(":support:cache"))

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
