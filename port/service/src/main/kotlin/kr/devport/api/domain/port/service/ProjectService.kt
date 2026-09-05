package kr.devport.api.domain.port.service

import kr.devport.api.domain.port.dto.response.ProjectDetailResponse
import kr.devport.api.domain.port.dto.response.ProjectEventResponse
import kr.devport.api.domain.port.enums.EventType
import kr.devport.api.domain.port.infrastructure.ProjectEventRepository
import kr.devport.api.domain.port.infrastructure.ProjectRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val projectEventRepository: ProjectEventRepository,
) {
    fun getProjectById(externalId: String): ProjectDetailResponse {
        val project =
            projectRepository
                .findByExternalId(externalId)
                .orElseThrow { IllegalArgumentException("Project not found with id: $externalId") }

        return project.toDetailResponse()
    }

    fun getProjectEvents(
        projectExternalId: String,
        eventType: EventType?,
        pageable: Pageable,
    ): Page<ProjectEventResponse> {
        val events =
            if (eventType != null) {
                projectEventRepository.findByProjectAndEventType(projectExternalId, eventType, pageable)
            } else {
                projectEventRepository.findByProject_ExternalId(projectExternalId, pageable)
            }

        return events.map { it.toEventResponse() }
    }
}
