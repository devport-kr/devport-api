package kr.devport.api.domain.wiki.controller

import kr.devport.api.domain.common.security.CustomUserDetails
import kr.devport.api.domain.wiki.dto.response.WikiMessageResponse
import kr.devport.api.domain.wiki.dto.response.WikiSessionListResponse
import kr.devport.api.domain.wiki.service.WikiChatSessionPersistenceService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Wiki chat session history endpoints. All require authentication. */
@RestController
@RequestMapping("/api/wiki/sessions")
class WikiSessionController(
    private val persistenceService: WikiChatSessionPersistenceService,
) {
    @GetMapping
    fun getAllSessions(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<WikiSessionListResponse> = ResponseEntity.ok(persistenceService.getUserSessions(userDetails.id, page, size))

    @GetMapping("/project")
    fun getProjectSessions(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestParam externalId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<WikiSessionListResponse> =
        ResponseEntity.ok(persistenceService.getProjectSessions(userDetails.id, externalId, page, size))

    @GetMapping("/global")
    fun getGlobalSessions(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<WikiSessionListResponse> = ResponseEntity.ok(persistenceService.getGlobalSessions(userDetails.id, page, size))

    @GetMapping("/{sessionId}/messages")
    fun getSessionMessages(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable sessionId: String,
    ): ResponseEntity<List<WikiMessageResponse>> = ResponseEntity.ok(persistenceService.loadSessionMessages(sessionId, userDetails.id))

    @DeleteMapping("/{sessionId}")
    fun deleteSession(
        @AuthenticationPrincipal userDetails: CustomUserDetails,
        @PathVariable sessionId: String,
    ): ResponseEntity<Void> {
        persistenceService.deleteSession(sessionId, userDetails.id)
        return ResponseEntity.noContent().build()
    }
}
