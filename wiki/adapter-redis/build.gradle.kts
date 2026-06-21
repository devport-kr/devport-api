// Driven adapter: implements the wiki session store + rate-limit counter over Redis.
dependencies {
    implementation(project(":wiki:infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}
