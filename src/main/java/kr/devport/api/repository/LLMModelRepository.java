package kr.devport.api.repository;

import kr.devport.api.domain.entity.LLMModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LLMModelRepository extends JpaRepository<LLMModel, Long> {

    // Find model by name
    Optional<LLMModel> findByName(String name);

    // Find model by name and provider
    Optional<LLMModel> findByNameAndProvider(String name, String provider);
}
