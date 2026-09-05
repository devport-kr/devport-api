// Out-ports owned by the llm core. The JPA adapters in :llm:repository-jpa implement these,
// so :llm:service depends only on the ports here — never on repository-jpa or Spring Data.
dependencies {
    api(project(":llm:model"))
}
