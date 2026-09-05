// Driven adapter: implements the wiki chat/embedding ports over the OpenAI API.
dependencies {
    implementation(project(":wiki:infrastructure"))

    implementation("com.openai:openai-java:${rootProject.libs.versions.openai.get()}") {
        exclude(group = "io.swagger.core.v3", module = "swagger-annotations")
    }
}
