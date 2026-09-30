package kr.devport.api.domain.gitrepo.repository;

import kr.devport.api.domain.gitrepo.entity.GitRepo;
import kr.devport.api.domain.gitrepo.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for GitHub repositories (GitRepo entity)
 * Spring Data JPA will automatically implement CRUD operations
 */
@Repository
public interface GitRepoRepository extends JpaRepository<GitRepo, Long> {

    /**
     * Find git repo by URL (unique constraint)
     */
    Optional<GitRepo> findByUrl(String url);

    /**
     * Find git repos by category with pagination
     */
    Page<GitRepo> findByCategory(Category category, Pageable pageable);

    /**
     * Find trending git repos ordered by stars gained this week
     */
    Page<GitRepo> findAllByOrderByStarsThisWeekDesc(Pageable pageable);

    /**
     * Find git repos by programming language
     */
    Page<GitRepo> findByLanguageOrderByScoreDesc(String language, Pageable pageable);
}
