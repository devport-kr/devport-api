package kr.devport.api.domain.llm.repository;

import kr.devport.api.domain.llm.entity.ImageToVideoModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageToVideoModelRepository extends JpaRepository<ImageToVideoModel, Long> {
}
