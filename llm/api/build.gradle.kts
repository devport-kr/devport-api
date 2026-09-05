// Primary adapter: REST controllers. Depends only on the service layer (never repository-jpa).
dependencies {
    implementation(project(":llm:service"))
}
