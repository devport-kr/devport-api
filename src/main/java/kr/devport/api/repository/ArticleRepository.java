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

    Page<Article> findAll(Pageable pageable);

    Page<Article> findByCategory(Category category, Pageable pageable);

    Page<Article> findBySource(Source source, Pageable pageable);

    Page<Article> findBySourceOrderByScoreDesc(Source source, Pageable pageable);

    List<Article> findAllByOrderByScoreDescCreatedAtSourceDesc(Pageable pageable);
}
