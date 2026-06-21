package kr.devport.api.domain.port.adapter.http

import kr.devport.api.domain.port.infrastructure.GitHubRepoFetcher
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/** HTTP adapter for [GitHubRepoFetcher] backed by the GitHub REST API. */
@Component
class GitHubRepoFetcherHttp : GitHubRepoFetcher {
    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    override fun fetch(
        owner: String,
        repo: String,
    ): Map<String, Any?> =
        try {
            val response =
                restTemplate.exchange(
                    "https://api.github.com/repos/$owner/$repo",
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<Map<String, Any?>>() {},
                )
            response.body ?: emptyMap()
        } catch (e: Exception) {
            log.warn("Failed to fetch GitHub repo {}/{}: {}", owner, repo, e.message)
            emptyMap()
        }
}
