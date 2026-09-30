package kr.devport.api.domain.auth.controller.admin;

import kr.devport.api.domain.auth.enums.UserRole;
import kr.devport.api.domain.auth.dto.response.UserResponse;
import kr.devport.api.domain.auth.service.admin.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserResponse> response = userAdminService.getAllUsers(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userAdminService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(
        @PathVariable Long id,
        @RequestParam UserRole role
    ) {
        UserResponse response = userAdminService.updateUserRole(id, role);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userAdminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
