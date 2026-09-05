package kr.devport.api.domain.auth.service.admin

import kr.devport.api.domain.auth.dto.UserResponse
import kr.devport.api.domain.auth.enums.UserRole
import kr.devport.api.domain.auth.infrastructure.UserRepository
import kr.devport.api.domain.auth.service.toUserResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserAdminService(
    private val userRepository: UserRepository,
) {
    fun getAllUsers(
        page: Int,
        size: Int,
    ): Page<UserResponse> =
        userRepository
            .findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
            .map { it.toUserResponse() }

    fun getUserById(id: Long): UserResponse =
        userRepository.findById(id).orElseThrow { IllegalArgumentException("User not found with id: $id") }.toUserResponse()

    @Transactional
    fun updateUserRole(
        id: Long,
        role: UserRole,
    ): UserResponse {
        val user = userRepository.findById(id).orElseThrow { IllegalArgumentException("User not found with id: $id") }
        user.role = role
        user.updatedAt = LocalDateTime.now()
        return userRepository.save(user).toUserResponse()
    }

    @Transactional
    fun deleteUser(id: Long) {
        require(userRepository.existsById(id)) { "User not found with id: $id" }
        userRepository.deleteById(id)
    }
}
