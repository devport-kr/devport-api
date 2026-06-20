// Article use-cases + all DTOs (Kotlin). ArticleLLMService wraps the OpenAI SDK.
dependencies {
    api(project(":article:model"))
    implementation(project(":article:repository-jpa"))

    api(project(":auth:model"))
    implementation(project(":auth:repository-jpa"))

    implementation(project(":support:cache"))
    implementation(project(":support:exception"))

    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("com.openai:openai-java:${rootProject.libs.versions.openai.get()}") {
        exclude(group = "io.swagger.core.v3", module = "swagger-annotations")
    }
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
