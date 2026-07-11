// Driven adapter: implements the auth OAuth2 exchange-code store over Redis.
dependencies {
    implementation(project(":auth:infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}
