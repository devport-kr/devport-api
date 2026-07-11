import com.linecorp.support.project.multi.recipe.configureByTypeHaving
import com.linecorp.support.project.multi.recipe.configureByTypePrefix
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    `java-library`
    `jvm-test-suite`
    alias(libs.plugins.build.recipe)
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.ktlint) apply false

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
}

allprojects {
    findProperty("group")?.let { group = it }
    version = "0.0.1-SNAPSHOT"
}

// =====================================================================================
// Build Recipe — module configuration is driven by the `type=` token in gradle.properties.
//   kotlin                       -> Kotlin JVM + ktlint + jvm-test-suite (test + integrationTest)
//   *-boot                       -> Spring Boot BOM + starter + jackson-kotlin (+ kotlin-spring)
//   *-boot-mvc                   -> Spring Web MVC + validation + springdoc + security-core
//   *-boot-jpa-repository        -> Spring Data JPA + hibernate-vector + kotlin-jpa (allopen/noarg)
//   *-boot-(mvc-)application     -> Spring Boot application plugin + actuator
// =====================================================================================

configureByTypePrefix("kotlin") {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply<KtlintPlugin>()

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    // Kotlin 2.2.x emits at most JVM 24 bytecode; align javac (compiled by the JDK 25
    // toolchain, emitting class version 24) so the two stay consistent.
    // `-parameters` is required so Spring MVC can resolve @PathVariable/@RequestParam names by
    // reflection (the Spring Boot plugin only adds it to the application module on its own).
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(24)
        options.compilerArgs.add("-parameters")
    }

    configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_24)
            javaParameters.set(true)
            freeCompilerArgs.addAll(
                "-Xjsr305=strict",
                "-Xjvm-default=all",
                "-opt-in=kotlin.RequiresOptIn",
                "-Xemit-jvm-type-annotations",
            )
        }
    }

    testing {
        suites {
            val test by getting(JvmTestSuite::class)
            val integrationTest by registering(JvmTestSuite::class)

            withType<JvmTestSuite> {
                useJUnitJupiter()
                targets {
                    all {
                        dependencies {
                            implementation(project())
                        }
                        testTask.configure {
                            shouldRunAfter(test)
                            testLogging {
                                events = mutableSetOf(TestLogEvent.FAILED)
                                exceptionFormat = TestExceptionFormat.FULL
                            }
                        }
                    }
                }
            }
        }
    }

    val integrationTestImplementation by configurations.getting {
        extendsFrom(configurations.testImplementation.get())
    }

    tasks {
        named("check") {
            dependsOn("integrationTest")
        }
        withType<Jar>().configureEach {
            project.parent?.takeIf { it != rootProject }?.let { domain ->
                archiveBaseName.set("${domain.name}-${project.name}")
            }
        }
    }

    dependencies {
        add("implementation", enforcedPlatform(rootProject.libs.kotlin.bom))
        add("implementation", kotlin("reflect"))
        add("implementation", kotlin("stdlib"))

        add("testImplementation", enforcedPlatform(SpringBootPlugin.BOM_COORDINATES))
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
    }
}

configureByTypeHaving("boot") {
    dependencies {
        add("implementation", enforcedPlatform(SpringBootPlugin.BOM_COORDINATES))
        add("implementation", "org.springframework.boot:spring-boot-starter")
        add("implementation", "tools.jackson.module:jackson-module-kotlin")
    }
}

configureByTypeHaving("kotlin", "boot") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
}

configureByTypeHaving("boot", "mvc") {
    dependencies {
        add("implementation", "org.springframework.boot:spring-boot-starter-web")
        add("implementation", "org.springframework.boot:spring-boot-starter-validation")
        add("implementation", "org.springframework.security:spring-security-core")
        add("implementation", "org.springdoc:springdoc-openapi-starter-webmvc-ui:${rootProject.libs.versions.springdoc.get()}")
    }
}

configureByTypeHaving("boot", "jpa") {
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    dependencies {
        add("api", "org.springframework.boot:spring-boot-starter-data-jpa")
        add("implementation", "org.hibernate.orm:hibernate-vector")
    }
}

configureByTypeHaving("boot", "jpa", "repository") {
    dependencies {
        add("testImplementation", "org.springframework.boot:spring-boot-starter-data-jpa-test")
    }
}

configureByTypeHaving("boot", "application") {
    apply(plugin = "org.springframework.boot")
    dependencies {
        add("implementation", "org.springframework.boot:spring-boot-starter-actuator")
    }
}
