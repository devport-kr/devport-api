// Controllers + the security/OAuth2/cookie web adapters (servlet-coupled, so they live here).
dependencies {
    implementation(project(":auth:service"))
    implementation(project(":auth:infrastructure"))
    implementation(project(":support:security"))
    implementation(project(":support:exception"))
    implementation(project(":support:logging"))

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
