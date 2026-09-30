package kr.devport.api.domain.port.repository;

import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.entity.QProject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectRepositoryImpl implements ProjectRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Project> findHotProjects(int limit) {
        QProject project = QProject.project;

        // Activity score: (releases_30d * 3) + (stars_week_delta / 100)
        NumberExpression<Integer> activityScore =
            project.releases30d.multiply(3)
                .add(project.starsWeekDelta.divide(100));

        return queryFactory
            .selectFrom(project)
            .orderBy(activityScore.desc())
            .limit(limit)
            .fetch();
    }
}
