package kr.devport.api.domain.wiki.service;

import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.ProjectRepository;
import kr.devport.api.domain.wiki.dto.response.WikiAdminProjectSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WikiAdminProjectQueryService {

    private final ProjectRepository projectRepository;

    public List<WikiAdminProjectSummaryResponse> listProjects() {
        return projectRepository.findAllForWikiAdmin().stream()
                .map(this::toSummary)
                .toList();
    }

    private WikiAdminProjectSummaryResponse toSummary(Project project) {
        return WikiAdminProjectSummaryResponse.builder()
                .projectId(project.getId())
                .projectExternalId(project.getExternalId())
                .fullName(project.getFullName())
                .stars(project.getStars() == null ? 0 : project.getStars())
                .language(project.getLanguage() == null || project.getLanguage().isBlank() ? "Unknown" : project.getLanguage())
                .build();
    }
}
