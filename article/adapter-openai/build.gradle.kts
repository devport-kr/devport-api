// Driven adapter: implements the article translator over the OpenAI Chat Completions API.
dependencies {
    implementation(project(":article:infrastructure"))
    implementation(project(":support:exception"))

    implementation("com.openai:openai-java:${rootProject.libs.versions.openai.get()}") {
        exclude(group = "io.swagger.core.v3", module = "swagger-annotations")
    }
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
