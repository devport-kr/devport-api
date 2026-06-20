package kr.devport.api.domain.port.repository

import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.entity.QProject
import org.springframework.stereotype.Repository

@Repository
class ProjectRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : ProjectRepositoryCustom {
    override fun findHotProjects(limit: Int): List<Project> {
        val project = QProject.project
        val activityScore: NumberExpression<Int> =
            project.releases30d
                .multiply(3)
                .add(project.starsWeekDelta.divide(100))

        return queryFactory
            .selectFrom(project)
            .orderBy(activityScore.desc())
            .limit(limit.toLong())
            .fetch()
    }
}
