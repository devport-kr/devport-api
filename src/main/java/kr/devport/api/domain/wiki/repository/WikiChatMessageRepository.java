package kr.devport.api.domain.wiki.repository;

import kr.devport.api.domain.wiki.entity.WikiChatMessage;
import kr.devport.api.domain.wiki.entity.WikiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface WikiChatMessageRepository extends JpaRepository<WikiChatMessage, Long> {

    List<WikiChatMessage> findBySessionOrderByCreatedAtAsc(WikiChatSession session);

    List<WikiChatMessage> findTop20BySessionOrderByCreatedAtDesc(WikiChatSession session);

    int countBySession(WikiChatSession session);

    @Query("SELECT m.session.id, COUNT(m) FROM WikiChatMessage m WHERE m.session IN :sessions GROUP BY m.session.id")
    List<Object[]> countBySessionsGrouped(@Param("sessions") List<WikiChatSession> sessions);

    default Map<Long, Long> countMapForSessions(List<WikiChatSession> sessions) {
        return countBySessionsGrouped(sessions).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }
}
