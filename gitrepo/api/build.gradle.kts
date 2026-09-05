// Primary Adapter: REST controllers + DTOs. Depends on the service in-port only (never the JPA adapter).
dependencies {
    implementation(project(":gitrepo:service"))
}
