package kr.devport.api.repository;

import kr.devport.api.domain.entity.Article;
import kr.devport.api.domain.enums.Category;
import kr.devport.api.domain.enums.Source;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // Find all articles with pagination
    Page<Article> findAll(Pageable pageable);

    // Find articles by category with pagination
    Page<Article> findByCategory(Category category, Pageable pageable);

    // Find articles by source with pagination
    Page<Article> findBySource(Source source, Pageable pageable);

    // Find GitHub articles sorted by score or stars
    Page<Article> findBySourceOrderByScoreDesc(Source source, Pageable pageable);

    // Find top N articles for trending ticker
    List<Article> findTopByOrderByScoreDescCreatedAtSourceDesc(Pageable pageable);
}
