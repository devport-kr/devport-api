dependencies {
    api(project(":wiki:model"))

    // pgvector similarity search via JdbcTemplate + raw SQL; jsonb metadata parsed with Jackson.
    implementation("org.springframework:spring-jdbc")
    implementation("com.fasterxml.jackson.core:jackson-databind")
}
