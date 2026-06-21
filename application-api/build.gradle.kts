import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

// =====================================================================================
// Composition root: assembles the migrated Hexagonal slices into the single bootable
// Spring Boot application (JVM).
// =====================================================================================

dependencies {
    implementation(project(":support:cache"))
    implementation(project(":support:security"))
    implementation(project(":support:exception"))
    implementation(project(":support:logging"))

    // auth — migrated Hexagonal slice (+ SecurityConfig lives in this composition root)
    implementation(project(":auth:api"))
    implementation(project(":auth:service"))
    implementation(project(":auth:repository-jpa"))
    implementation(project(":auth:adapter-redis"))
    implementation(project(":auth:adapter-http"))
    implementation(project(":auth:infrastructure"))
    implementation(project(":auth:model"))

    // gitrepo — migrated Hexagonal slice (adapters + use-cases composed here)
    implementation(project(":gitrepo:api"))
    implementation(project(":gitrepo:service"))
    implementation(project(":gitrepo:repository-jpa"))
    implementation(project(":gitrepo:infrastructure"))
    implementation(project(":gitrepo:model"))

    implementation(project(":llm:api"))
    implementation(project(":llm:service"))
    implementation(project(":llm:repository-jpa"))
    implementation(project(":llm:model"))

    implementation(project(":article:api"))
    implementation(project(":article:service"))
    implementation(project(":article:repository-jpa"))
    implementation(project(":article:adapter-openai"))
    implementation(project(":article:infrastructure"))
    implementation(project(":article:model"))

    implementation(project(":mypage:api"))
    implementation(project(":mypage:service"))
    implementation(project(":mypage:repository-jpa"))
    implementation(project(":mypage:model"))

    implementation(project(":port:api"))
    implementation(project(":port:service"))
    implementation(project(":port:repository-jpa"))
    implementation(project(":port:adapter-http"))
    implementation(project(":port:infrastructure"))
    implementation(project(":port:model"))

    implementation(project(":wiki:api"))
    implementation(project(":wiki:service"))
    implementation(project(":wiki:repository-jpa"))
    implementation(project(":wiki:model"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.openai:openai-java:${rootProject.libs.versions.openai.get()}") {
        exclude(group = "io.swagger.core.v3", module = "swagger-annotations")
    }
    implementation("com.querydsl:querydsl-jpa:${rootProject.libs.versions.querydsl.get()}:jakarta")
    implementation("com.zaxxer:HikariCP")

    developmentOnly(platform("org.springframework.boot:spring-boot-dependencies:${rootProject.libs.versions.spring.boot.get()}"))
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")

    // Test suite (full application context) lives here.
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-client-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
}

springBoot {
    mainClass.set("kr.devport.api.DevportApiApplicationKt")
}

tasks.named<BootBuildImage>("bootBuildImage") {
    builder.set("paketobuildpacks/builder-noble-java-tiny:0.0.124")
    buildpacks.set(
        listOf(
            "urn:cnb:builder:paketo-buildpacks/java",
            "docker.io/paketobuildpacks/health-checker",
        ),
    )
    environment.set(
        mapOf(
            "BP_HEALTH_CHECKER_ENABLED" to "true",
            "BP_JVM_VERSION" to "25",
        ),
    )
}

// Opt-in wiki RAG evaluation harness (real Postgres). Run with `./gradlew :application-api:ragEval`.
tasks.register<Test>("ragEval") {
    group = "verification"
    description = "Runs the opt-in wiki RAG evaluation harness against a real Postgres database."
    useJUnitPlatform {
        includeTags("rag-eval")
    }
    systemProperty("rag-eval", "true")
    systemProperty("spring.profiles.active", "rag-eval")
    shouldRunAfter(tasks.named("test"))
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/*RagEvalRunner*")
}
