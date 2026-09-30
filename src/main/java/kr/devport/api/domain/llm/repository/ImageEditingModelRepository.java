package kr.devport.api.domain.llm.repository;

import kr.devport.api.domain.llm.entity.ImageEditingModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageEditingModelRepository extends JpaRepository<ImageEditingModel, Long> {
}
