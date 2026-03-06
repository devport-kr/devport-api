package kr.devport.api.domain.port.service.admin;

import kr.devport.api.domain.port.dto.request.admin.ProjectCreateRequest;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@Slf4j
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

        String[] parts = fullName.split("/", 2);
        Map<String, Object> gh = parts.length == 2
                ? fetchGitHubRepo(parts[0], parts[1])
                : Collections.emptyMap();

        String repoUrl = request.getRepoUrl() != null ? request.getRepoUrl()
                : (String) gh.getOrDefault("html_url", "https://github.com/" + fullName);

        String homepageUrl = request.getHomepageUrl() != null ? request.getHomepageUrl()
                : (String) gh.get("homepage");

        String description = request.getDescription() != null ? request.getDescription()
                : (String) gh.get("description");

        int stars = request.getStars() != null ? request.getStars()
                : toInt(gh.get("stargazers_count"));

        int forks = request.getForks() != null ? request.getForks()
                : toInt(gh.get("forks_count"));

        String language = request.getLanguage() != null ? request.getLanguage()
                : (String) gh.get("language");

        String license = request.getLicense() != null ? request.getLicense()
                : extractLicense(gh.get("license"));

        Project project = Project.builder()
                .externalId(externalId)
                .name(name)
                .fullName(fullName)
                .repoUrl(repoUrl)
                .homepageUrl(homepageUrl)
                .description(description)
                .stars(stars)
                .forks(forks)
                .language(language)
                .license(license)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return projectRepository.save(project);
    }

    private Map<String, Object> fetchGitHubRepo(String owner, String repo) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://api.github.com/repos/" + owner + "/" + repo,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();
            return body != null ? body : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("Failed to fetch GitHub repo {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private int toInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        return 0;
    }

    @SuppressWarnings("unchecked")
    private String extractLicense(Object licenseObj) {
        if (licenseObj instanceof Map) {
            Object spdxId = ((Map<String, Object>) licenseObj).get("spdx_id");
            if (spdxId instanceof String) return (String) spdxId;
        }
        return null;
    }
}
