// My-page use-cases + DTOs. Reads Article via :article:repository-jpa and User via :auth:repository-jpa.
dependencies {
    api(project(":mypage:model"))
    implementation(project(":mypage:repository-jpa"))
    implementation(project(":article:repository-jpa"))
    implementation(project(":auth:repository-jpa"))
}
