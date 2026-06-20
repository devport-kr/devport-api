package kr.devport.api.domain.auth.controller.admin

import kr.devport.api.domain.auth.dto.UserResponse
import kr.devport.api.domain.auth.enums.UserRole
import kr.devport.api.domain.auth.service.admin.UserAdminService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/users")
class UserAdminController(
    private val userAdminService: UserAdminService,
) {
    @GetMapping
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<UserResponse>> = ResponseEntity.ok(userAdminService.getAllUsers(page, size))

    @GetMapping("/{id}")
    fun getUserById(
        @PathVariable id: Long,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userAdminService.getUserById(id))

    @PatchMapping("/{id}/role")
    fun updateUserRole(
        @PathVariable id: Long,
        @RequestParam role: UserRole,
    ): ResponseEntity<UserResponse> = ResponseEntity.ok(userAdminService.updateUserRole(id, role))

    @DeleteMapping("/{id}")
    fun deleteUser(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        userAdminService.deleteUser(id)
        return ResponseEntity.noContent().build()
    }
}
