package kr.devport.api.domain.article.controller.admin;

import jakarta.validation.Valid;
import kr.devport.api.domain.article.dto.request.admin.ArticleCreateRequest;
import kr.devport.api.domain.article.dto.request.admin.ArticleLLMCreateRequest;
import kr.devport.api.domain.article.dto.request.admin.ArticleUpdateRequest;
import kr.devport.api.domain.article.dto.response.ArticleLLMPreviewResponse;
import kr.devport.api.domain.article.dto.response.ArticlePageResponse;
import kr.devport.api.domain.article.dto.response.ArticleResponse;
import kr.devport.api.domain.article.service.admin.ArticleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class ArticleAdminController {

    private final ArticleAdminService articleAdminService;

    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody ArticleCreateRequest request) {
        ArticleResponse response = articleAdminService.createArticle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/llm-process")
    public ResponseEntity<ArticleResponse> processArticleWithLLM(@Valid @RequestBody ArticleLLMCreateRequest request) {
        ArticleResponse response = articleAdminService.createArticleFromLLM(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/llm-preview")
    public ResponseEntity<ArticleLLMPreviewResponse> previewArticleLLM(@Valid @RequestBody ArticleLLMCreateRequest request) {
        ArticleLLMPreviewResponse response = articleAdminService.previewArticleLLM(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ArticlePageResponse> listArticles(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String search
    ) {
        ArticlePageResponse response = articleAdminService.listArticles(page, size, search);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(
        @PathVariable Long id,
        @Valid @RequestBody ArticleUpdateRequest request
    ) {
        ArticleResponse response = articleAdminService.updateArticle(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        articleAdminService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
