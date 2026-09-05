// My-page use-cases + DTOs. Depends only on ports — resolves articles via article's ArticleDirectory.
dependencies {
    api(project(":mypage:model"))
    implementation(project(":mypage:infrastructure"))
    implementation(project(":article:infrastructure"))
}
