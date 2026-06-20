// Use-cases + DTOs. Depends only on the out-ports (:llm:infrastructure) — never on repository-jpa.
dependencies {
    api(project(":llm:model"))
    implementation(project(":llm:infrastructure"))
    implementation(project(":support:cache"))
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
