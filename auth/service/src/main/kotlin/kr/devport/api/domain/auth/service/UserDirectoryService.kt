package kr.devport.api.domain.auth.service

import kr.devport.api.domain.auth.UserSummary
import kr.devport.api.domain.auth.entity.User
import kr.devport.api.domain.auth.infrastructure.UserDirectory
import kr.devport.api.domain.auth.infrastructure.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Implements the cross-domain inbound port: resolves user ids to [UserSummary] for other domains. */
@Service
@Transactional(readOnly = true)
class UserDirectoryService(
    private val userRepository: UserRepository,
) : UserDirectory {
    override fun findById(userId: Long): UserSummary? = userRepository.findById(userId).map { it.toSummary() }.orElse(null)

    override fun findByIds(userIds: Collection<Long>): Map<Long, UserSummary> {
        if (userIds.isEmpty()) return emptyMap()
        return userRepository.findAllByIdIn(userIds).associate { it.id!! to it.toSummary() }
    }
}

private fun User.toSummary(): UserSummary =
    UserSummary(
        id = id!!,
        username = username,
        name = name,
        email = email,
        profileImageUrl = profileImageUrl,
        flair = flair,
        flairColor = flairColor,
        role = role,
    )
