package kr.devport.api.domain.article.service;

import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.article.entity.ArticleComment;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.article.dto.request.CommentCreateRequest;
import kr.devport.api.domain.article.dto.request.CommentUpdateRequest;
import kr.devport.api.domain.article.dto.response.CommentResponse;
import kr.devport.api.domain.article.repository.ArticleCommentRepository;
import kr.devport.api.domain.article.repository.ArticleRepository;
import kr.devport.api.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final ArticleCommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    public List<CommentResponse> getCommentsByArticle(String articleExternalId, Long currentUserId) {
        List<ArticleComment> comments = commentRepository.findAllByArticleExternalId(articleExternalId);
        return comments.stream()
            .map(c -> CommentResponse.from(c, currentUserId))
            .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse createComment(
        String articleExternalId,
        CommentCreateRequest request,
        Long userId
    ) {
        Article article = articleRepository.findByExternalId(articleExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Article not found: " + articleExternalId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        ArticleComment comment = ArticleComment.builder()
            .article(article)
            .user(user)
            .content(request.getContent())
            .build();

        if (request.getParentCommentId() != null) {
            ArticleComment parent = commentRepository.findByExternalId(request.getParentCommentId())
                .orElseThrow(() -> new IllegalArgumentException("Parent comment not found: " + request.getParentCommentId()));

            if (!parent.getArticle().getId().equals(article.getId())) {
                throw new IllegalArgumentException("Parent comment does not belong to this article");
            }
            comment.setParentComment(parent);
        }

        ArticleComment saved = commentRepository.save(comment);
        return CommentResponse.from(saved, userId);
    }

    @Transactional
    public CommentResponse updateComment(String commentExternalId, CommentUpdateRequest request, Long userId) {
        ArticleComment comment = commentRepository.findByExternalId(commentExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentExternalId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only edit your own comments");
        }

        if (comment.getDeleted()) {
            throw new IllegalArgumentException("Cannot edit a deleted comment");
        }

        comment.setContent(request.getContent());
        ArticleComment updated = commentRepository.save(comment);
        return CommentResponse.from(updated, userId);
    }

    @Transactional
    public void deleteComment(String commentExternalId, Long userId) {
        ArticleComment comment = commentRepository.findByExternalId(commentExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentExternalId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own comments");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
    }
}
