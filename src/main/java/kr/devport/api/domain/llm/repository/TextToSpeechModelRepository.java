package kr.devport.api.domain.llm.repository;

import kr.devport.api.domain.llm.entity.TextToSpeechModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TextToSpeechModelRepository extends JpaRepository<TextToSpeechModel, Long> {
}
