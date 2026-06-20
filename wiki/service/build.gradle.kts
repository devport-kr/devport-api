// Wiki use-cases, DTOs, Redis-backed session store, and RAG/OpenAI orchestration (Kotlin).
dependencies {
    api(project(":wiki:model"))
    implementation(project(":wiki:repository-jpa"))

    implementation(project(":auth:model"))
    implementation(project(":auth:repository-jpa"))
    implementation(project(":port:model"))
    implementation(project(":port:repository-jpa"))
    implementation(project(":support:cache"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.openai:openai-java:${rootProject.libs.versions.openai.get()}") {
        exclude(group = "io.swagger.core.v3", module = "swagger-annotations")
    }
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
