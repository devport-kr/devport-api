package kr.devport.api.domain.llm.repository;

import kr.devport.api.domain.llm.entity.TextToVideoModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextToVideoModelRepository extends JpaRepository<TextToVideoModel, Long> {
}
