// Port domain + persistence model. QueryDSL APT generates QProject for :port:repository-jpa.
// Comments/votes reference users by id (cross-domain), so no :auth:model dependency.
apply(plugin = "org.jetbrains.kotlin.kapt")

dependencies {
    api("com.querydsl:querydsl-jpa:${rootProject.libs.versions.querydsl.get()}:jakarta")
    "kapt"(platform("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.boot.get()}"))
    "kapt"("com.querydsl:querydsl-apt:${rootProject.libs.versions.querydsl.get()}:jakarta")
    "kapt"("jakarta.annotation:jakarta.annotation-api")
    "kapt"("jakarta.persistence:jakarta.persistence-api")
}
