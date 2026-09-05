package kr.devport.api.domain.common.cache.admin

import kr.devport.api.domain.common.cache.CacheFallbackStateStore
import kr.devport.api.domain.common.cache.CacheScope
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Admin/internal-only cache fallback controls (restricted to admin routes by SecurityConfig).
 * Inspect and override fallback uncertainty state. Do NOT expose equivalent public endpoints.
 */
@RestController
@RequestMapping("/api/admin/cache")
class CacheAdminController(
    private val stateStore: CacheFallbackStateStore,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/fallback/status")
    fun getFallbackStatus(): ResponseEntity<Map<String, ScopeStatus>> {
        val status = HashMap<String, ScopeStatus>()
        var uncertainScopeCount = 0

        for (scope in CacheScope.entries) {
            val uncertain = stateStore.isUncertain(scope)
            val jobId = stateStore.getUncertainJobId(scope)
            if (uncertain) {
                uncertainScopeCount++
            }
            status[scope.name] = ScopeStatus(uncertain, jobId)
        }

        log.debug("Admin queried fallback status for {} scopes (uncertainScopes={})", status.size, uncertainScopeCount)
        return ResponseEntity.ok(status)
    }

    @GetMapping("/fallback/status/{scope}")
    fun getFallbackStatusForScope(
        @PathVariable scope: String,
    ): ResponseEntity<ScopeStatusDetail> =
        try {
            val cacheScope = CacheScope.valueOf(scope.uppercase())
            val uncertain = stateStore.isUncertain(cacheScope)
            val jobId = stateStore.getUncertainJobId(cacheScope)

            log.debug("Admin queried fallback status for scope={}: uncertain={}, jobId={}", scope, uncertain, jobId)
            ResponseEntity.ok(ScopeStatusDetail(cacheScope.name, uncertain, jobId))
        } catch (e: IllegalArgumentException) {
            log.warn("Admin requested invalid scope: {}", scope)
            ResponseEntity.badRequest().build()
        }

    @PostMapping("/fallback/override/{scope}")
    fun markScopeUncertain(
        @PathVariable scope: String,
        @RequestBody request: OverrideRequest,
    ): ResponseEntity<OverrideResult> =
        try {
            val cacheScope = CacheScope.valueOf(scope.uppercase())
            val jobId = request.jobId ?: "manual-override"

            stateStore.markUncertain(cacheScope, jobId)

            log.info("Admin manually marked scope={} as uncertain with jobId={}", scope, jobId)
            ResponseEntity.ok(OverrideResult(cacheScope.name, "marked_uncertain", jobId))
        } catch (e: IllegalArgumentException) {
            log.warn("Admin requested invalid scope for override: {}", scope)
            ResponseEntity.badRequest().build()
        }

    @DeleteMapping("/fallback/override/{scope}")
    fun clearScopeUncertainty(
        @PathVariable scope: String,
    ): ResponseEntity<OverrideResult> =
        try {
            val cacheScope = CacheScope.valueOf(scope.uppercase())
            val jobId = "manual-clear"

            stateStore.clearUncertainty(cacheScope, jobId)

            log.info("Admin manually cleared uncertainty for scope={}", scope)
            ResponseEntity.ok(OverrideResult(cacheScope.name, "cleared_uncertainty", jobId))
        } catch (e: IllegalArgumentException) {
            log.warn("Admin requested invalid scope for clear: {}", scope)
            ResponseEntity.badRequest().build()
        }

    data class ScopeStatus(
        val uncertain: Boolean,
        val jobId: String?,
    )

    data class ScopeStatusDetail(
        val scope: String,
        val uncertain: Boolean,
        val jobId: String?,
    )

    data class OverrideRequest(
        val jobId: String? = null,
    )

    data class OverrideResult(
        val scope: String,
        val action: String,
        val jobId: String?,
    )
}
