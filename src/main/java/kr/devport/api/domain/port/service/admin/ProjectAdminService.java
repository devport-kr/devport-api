package kr.devport.api.domain.port.service.admin;

import kr.devport.api.domain.port.dto.request.admin.ProjectCreateRequest;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectAdminService {

    private final ProjectRepository projectRepository;

    @Transactional
    public Project createProject(ProjectCreateRequest request) {
        String fullName = request.getFullName().trim();
        String externalId = "github:" + fullName.toLowerCase();

        if (projectRepository.findByExternalId(externalId).isPresent()) {
            throw new IllegalStateException("Project already exists: " + fullName);
        }

        String name = fullName.contains("/") ? fullName.substring(fullName.indexOf('/') + 1) : fullName;

        Project project = Project.builder()
                .externalId(externalId)
                .name(name)
                .fullName(fullName)
                .repoUrl(request.getRepoUrl())
                .homepageUrl(request.getHomepageUrl())
                .description(request.getDescription())
                .stars(request.getStars() != null ? request.getStars() : 0)
                .forks(request.getForks() != null ? request.getForks() : 0)
                .language(request.getLanguage())
                .license(request.getLicense())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return projectRepository.save(project);
    }
}
