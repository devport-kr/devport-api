package kr.devport.api.domain.port.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import kr.devport.api.domain.port.dto.request.admin.ProjectCreateRequest;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.service.admin.ProjectAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProjectAdminController {

    private final ProjectAdminService projectAdminService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        Project project = projectAdminService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", project.getId(),
                "externalId", project.getExternalId(),
                "fullName", project.getFullName()
        ));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> createProjects(
            @RequestBody @Size(min = 1, max = 100, message = "Provide between 1 and 100 projects")
            List<@Valid ProjectCreateRequest> requests) {

        List<Map<String, Object>> created = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();

        for (ProjectCreateRequest request : requests) {
            try {
                Project project = projectAdminService.createProject(request);
                created.add(Map.of(
                        "id", project.getId(),
                        "externalId", project.getExternalId(),
                        "fullName", project.getFullName()
                ));
            } catch (Exception e) {
                failed.add(Map.of(
                        "fullName", request.getFullName(),
                        "error", e.getMessage()
                ));
            }
        }

        HttpStatus status = failed.isEmpty() ? HttpStatus.CREATED : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(Map.of(
                "created", created,
                "failed", failed
        ));
    }
}
