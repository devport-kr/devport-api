// In-Port (use-case) interfaces + internal implementations. Knows the out-port, not the adapter.
dependencies {
    api(project(":gitrepo:model"))
    implementation(project(":gitrepo:infrastructure"))
    implementation(project(":support:cache"))

    // @Transactional (spring-tx) + @Cacheable (spring-context, via starter)
    implementation("org.springframework:spring-tx")
}
