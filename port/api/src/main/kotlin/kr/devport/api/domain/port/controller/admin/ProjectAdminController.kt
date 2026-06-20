package kr.devport.api.domain.port.controller.admin

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import kr.devport.api.domain.port.dto.request.admin.ProjectCreateRequest
import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.service.admin.ProjectAdminService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/admin/projects")
@PreAuthorize("hasRole('ADMIN')")
class ProjectAdminController(
    private val projectAdminService: ProjectAdminService,
) {
    @PostMapping
    fun createProject(
        @Valid @RequestBody request: ProjectCreateRequest,
    ): ResponseEntity<Map<String, Any?>> {
        val project = projectAdminService.createProject(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(project.toAdminResponse())
    }

    @PostMapping("/bulk")
    fun createProjects(
        @RequestBody
        @Size(min = 1, max = 100, message = "Provide between 1 and 100 projects")
        requests: List<@Valid ProjectCreateRequest>,
    ): ResponseEntity<Map<String, Any>> {
        val created = mutableListOf<Map<String, Any?>>()
        val failed = mutableListOf<Map<String, Any?>>()

        for (request in requests) {
            try {
                created += projectAdminService.createProject(request).toAdminResponse()
            } catch (e: Exception) {
                failed +=
                    mapOf(
                        "fullName" to request.fullName,
                        "error" to e.message,
                    )
            }
        }

        val status = if (failed.isEmpty()) HttpStatus.CREATED else HttpStatus.MULTI_STATUS
        return ResponseEntity
            .status(status)
            .body(
                mapOf(
                    "created" to created,
                    "failed" to failed,
                ),
            )
    }

    private fun Project.toAdminResponse(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "externalId" to externalId,
            "fullName" to fullName,
        )
}
