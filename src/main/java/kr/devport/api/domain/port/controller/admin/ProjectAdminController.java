package kr.devport.api.domain.port.controller.admin;

import jakarta.validation.Valid;
import kr.devport.api.domain.port.dto.request.admin.ProjectCreateRequest;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.service.admin.ProjectAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
}
