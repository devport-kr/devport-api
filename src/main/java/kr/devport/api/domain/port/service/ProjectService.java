package kr.devport.api.domain.port.service;

import kr.devport.api.domain.port.dto.response.ProjectDetailResponse;
import kr.devport.api.domain.port.dto.response.ProjectEventResponse;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.entity.ProjectEvent;
import kr.devport.api.domain.port.enums.EventType;
import kr.devport.api.domain.port.repository.ProjectEventRepository;
import kr.devport.api.domain.port.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectEventRepository projectEventRepository;

    @Transactional(readOnly = true)
    public ProjectDetailResponse getProjectById(String externalId) {
        Project project = projectRepository.findByExternalId(externalId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + externalId));

        return ProjectDetailResponse.builder()
            .id(project.getExternalId())
            .name(project.getName())
            .fullName(project.getFullName())
            .repoUrl(project.getRepoUrl())
            .homepageUrl(project.getHomepageUrl())
            .description(project.getDescription())
            .stars(project.getStars())
            .forks(project.getForks())
            .contributors(project.getContributors())
            .language(project.getLanguage())
            .languageColor(project.getLanguageColor())
            .license(project.getLicense())
            .lastRelease(project.getLastRelease())
            .tags(project.getTags())
            .build();
    }

    @Transactional(readOnly = true)
    public Page<ProjectEventResponse> getProjectEvents(String projectExternalId, EventType eventType, Pageable pageable) {
        Page<ProjectEvent> events;

        if (eventType != null) {
            events = projectEventRepository.findByProjectAndEventType(projectExternalId, eventType, pageable);
        } else {
            events = projectEventRepository.findByProject_ExternalId(projectExternalId, pageable);
        }

        return events.map(this::toEventResponse);
    }

    private ProjectEventResponse toEventResponse(ProjectEvent event) {
        return ProjectEventResponse.builder()
            .id(event.getExternalId())
            .version(event.getVersion())
            .releasedAt(event.getReleasedAt())
            .eventTypes(event.getEventTypes())
            .summary(event.getSummary())
            .bullets(event.getBullets())
            .impactScore(event.getImpactScore())
            .isSecurity(event.getIsSecurity())
            .isBreaking(event.getIsBreaking())
            .sourceUrl(event.getSourceUrl())
            .build();
    }
}
