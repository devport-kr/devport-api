// My-page persistence model (UserSavedArticle, UserReadHistory). Each links a User (auth) to an
// Article (article). No QueryDSL (derived queries only), so no kapt.
dependencies {
    api(project(":article:model"))
    api(project(":auth:model"))
}
