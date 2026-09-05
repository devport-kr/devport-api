package kr.devport.api.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArchitectureBoundaryTest {
    private val sourceFiles: List<KoFileDeclaration> by lazy {
        Konsist
            .scopeFromProject()
            .files
            .filter { it.normalizedPath.contains("/src/main/kotlin/") }
    }

    @Test
    fun `service layer does not import repositories adapters or web APIs`() {
        val forbiddenPrefixes =
            listOf(
                "com.openai.",
                "org.springframework.data.redis.",
                "org.springframework.web.",
                "jakarta.servlet.",
            )
        val violations =
            sourceFiles
                .filter { it.isLayer("service") }
                .flatMap { file ->
                    file.imports.mapNotNull { import ->
                        val name = import.name
                        val forbidden =
                            ".repository." in name ||
                                name == "org.springframework.data.jpa.repository.JpaRepository" ||
                                forbiddenPrefixes.any { name.startsWith(it) }
                        if (forbidden) "${file.projectPath}: $name" else null
                    }
                }

        assertThat(violations)
            .describedAs("service modules should depend on ports, not repositories/adapters/web APIs")
            .isEmpty()
    }

    @Test
    fun `api layer does not import repositories`() {
        val violations =
            sourceFiles
                .filter { it.isLayer("api") }
                .flatMap { file ->
                    file.imports
                        .map { it.name }
                        .filter { ".repository." in it }
                        .map { "${file.projectPath}: $it" }
                }

        assertThat(violations)
            .describedAs("api modules should talk to service layer APIs, not repositories")
            .isEmpty()
    }

    @Test
    fun `domain model modules do not import other domains`() {
        val violations =
            sourceFiles
                .filter { it.isLayer("model") }
                .flatMap { file ->
                    val ownerDomain = file.ownerDomain() ?: return@flatMap emptyList()
                    file.imports
                        .map { it.name }
                        .mapNotNull { importName ->
                            val importedDomain = importName.domainImportSegment() ?: return@mapNotNull null
                            if (importedDomain != ownerDomain) {
                                "${file.projectPath}: $importName"
                            } else {
                                null
                            }
                        }
                }

        assertThat(violations)
            .describedAs("model modules should not import other domain models")
            .isEmpty()
    }

    private fun KoFileDeclaration.isLayer(layer: String): Boolean = normalizedPath.contains("/$layer/src/main/kotlin/")

    private fun KoFileDeclaration.ownerDomain(): String? {
        val marker = "/model/src/main/kotlin/"
        val beforeMarker = normalizedPath.substringBefore(marker, missingDelimiterValue = "")
        return beforeMarker.substringAfterLast("/", missingDelimiterValue = "").takeIf { it.isNotBlank() }
    }

    private fun String.domainImportSegment(): String? {
        val prefix = "kr.devport.api.domain."
        if (!startsWith(prefix)) {
            return null
        }
        val domain = removePrefix(prefix).substringBefore(".")
        return domain.takeIf { it in domainNames }
    }

    private val KoFileDeclaration.normalizedPath: String
        get() = path.replace('\\', '/')

    companion object {
        private val domainNames = setOf("auth", "article", "gitrepo", "llm", "mypage", "port", "wiki")
    }
}
