// Shared logging utilities (log sanitization + MDC context propagation).
dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.boot.get()}"))
    api("org.slf4j:slf4j-api")
}
