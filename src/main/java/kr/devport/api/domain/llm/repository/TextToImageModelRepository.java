package kr.devport.api.domain.llm.repository;

import kr.devport.api.domain.llm.entity.TextToImageModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextToImageModelRepository extends JpaRepository<TextToImageModel, Long> {
}
