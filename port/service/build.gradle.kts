// Port use-cases + DTOs.
dependencies {
    api(project(":port:model"))
    implementation(project(":port:repository-jpa"))
    implementation(project(":auth:repository-jpa"))

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework:spring-web")
}
