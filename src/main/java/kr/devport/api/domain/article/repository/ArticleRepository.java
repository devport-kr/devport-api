package kr.devport.api.domain.article.repository;

import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.article.enums.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long>, ArticleRepositoryCustom {

    Optional<Article> findByExternalId(String externalId);

    Page<Article> findAll(Pageable pageable);

    Page<Article> findByCategory(Category category, Pageable pageable);

    Page<Article> findBySource(String source, Pageable pageable);

    Page<Article> findBySourceOrderByScoreDesc(String source, Pageable pageable);

    List<Article> findAllByOrderByScoreDescCreatedAtSourceDesc(Pageable pageable);
}
