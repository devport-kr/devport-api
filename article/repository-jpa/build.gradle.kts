dependencies {
    api(project(":article:model"))
    implementation(project(":article:infrastructure"))

    // Title autocomplete + capped counts via JdbcTemplate + raw SQL.
    implementation("org.springframework:spring-jdbc")
}
