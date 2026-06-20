// Use-cases + DTOs. Depends on the persistence adapter (Spring Data repos) and exposes the model.
dependencies {
    api(project(":llm:model"))
    implementation(project(":llm:repository-jpa"))
    implementation(project(":support:cache"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
