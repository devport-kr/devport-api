package kr.devport.api.domain.port.infrastructure

import kr.devport.api.domain.port.ProjectView

/**
 * Inbound port: the port core's published contract for *other* domains (wiki). Replaces cross-domain
 * reach into port's entities/repositories — callers resolve projects to [ProjectView] here.
 * Implemented by :port:service; consumers depend only on this interface (in :port:infrastructure).
 */
interface ProjectDirectory {
    fun listAllByStarsDesc(): List<ProjectView>

    fun listForWikiAdmin(): List<ProjectView>

    fun findByExternalId(externalId: String): ProjectView?
}
