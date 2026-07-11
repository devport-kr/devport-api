package kr.devport.api.domain.auth.infrastructure

import kr.devport.api.domain.auth.UserSummary

/**
 * Inbound port: the auth core's published contract for *other* domains. Replaces cross-domain
 * reach into auth's entities/repositories — callers reference users by id and resolve [UserSummary]s
 * here. Implemented by :auth:service; consumers depend only on this interface (in :auth:infrastructure).
 */
interface UserDirectory {
    fun findById(userId: Long): UserSummary?

    fun findByIds(userIds: Collection<Long>): Map<Long, UserSummary>
}
