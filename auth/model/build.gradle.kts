// Auth domain + persistence model (User, tokens). Shared kernel: other domains reference User via :auth:model.
// QueryDSL APT (kapt) generates QUser etc. for repositories that join on User across modules.
apply(plugin = "org.jetbrains.kotlin.kapt")

dependencies {
    api("com.querydsl:querydsl-jpa:${rootProject.libs.versions.querydsl.get()}:jakarta")
    "kapt"(platform("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.boot.get()}"))
    "kapt"("com.querydsl:querydsl-apt:${rootProject.libs.versions.querydsl.get()}:jakarta")
    "kapt"("jakarta.annotation:jakarta.annotation-api")
    "kapt"("jakarta.persistence:jakarta.persistence-api")
}
