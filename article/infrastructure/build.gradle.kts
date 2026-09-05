// Out-ports owned by the article core: persistence (repo/comment) + the article translator (OpenAI).
dependencies {
    api(project(":article:model"))
}
