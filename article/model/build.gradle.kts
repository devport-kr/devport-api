// Article domain + persistence model (Article, ArticleComment, ArticleMetadata, enums).
// QueryDSL APT (kapt) generates QArticle etc. for the type-safe dynamic search in :article:repository-jpa.
// QArticleComment references User, so :auth:model is on the kapt/compile classpath for QUser.
apply(plugin = "org.jetbrains.kotlin.kapt")

dependencies {
    api(project(":auth:model"))

    api("com.querydsl:querydsl-jpa:${rootProject.libs.versions.querydsl.get()}:jakarta")
    "kapt"(platform("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.boot.get()}"))
    "kapt"("com.querydsl:querydsl-apt:${rootProject.libs.versions.querydsl.get()}:jakarta")
    "kapt"("jakarta.annotation:jakarta.annotation-api")
    "kapt"("jakarta.persistence:jakarta.persistence-api")
}
