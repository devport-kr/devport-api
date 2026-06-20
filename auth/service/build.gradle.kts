// Auth use-cases, OAuth2, JWT filter, email, DTOs.
dependencies {
    api(project(":auth:model"))
    implementation(project(":auth:repository-jpa"))
    implementation(project(":support:security"))
    implementation(project(":support:exception"))
    implementation(project(":support:logging"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
