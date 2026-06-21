// Driven adapter: implements captcha verification over HTTP (Cloudflare Turnstile).
dependencies {
    implementation(project(":auth:infrastructure"))
    implementation("org.springframework:spring-web")
}
