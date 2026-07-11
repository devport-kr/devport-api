package kr.devport.api.domain.port.repository

import kr.devport.api.domain.port.entity.Project

interface ProjectRepositoryCustom {
    fun findHotProjects(limit: Int): List<Project>
}
