// Secondary adapter: Spring Data JPA repositories. The api layer cannot depend on this module.
dependencies {
    api(project(":llm:model"))
    implementation(project(":llm:infrastructure"))
}
