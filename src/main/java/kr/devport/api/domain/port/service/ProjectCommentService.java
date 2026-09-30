package kr.devport.api.domain.port.service;

import kr.devport.api.domain.article.dto.request.CommentCreateRequest;
import kr.devport.api.domain.article.dto.request.CommentUpdateRequest;
import kr.devport.api.domain.article.dto.response.CommentAuthorResponse;
import kr.devport.api.domain.auth.entity.User;
import kr.devport.api.domain.auth.repository.UserRepository;
import kr.devport.api.domain.port.dto.request.VoteRequest;
import kr.devport.api.domain.port.dto.response.ProjectCommentResponse;
import kr.devport.api.domain.port.dto.response.VoteResponse;
import kr.devport.api.domain.port.entity.Project;
import kr.devport.api.domain.port.entity.ProjectComment;
import kr.devport.api.domain.port.entity.ProjectCommentVote;
import kr.devport.api.domain.port.repository.ProjectCommentRepository;
import kr.devport.api.domain.port.repository.ProjectCommentVoteRepository;
import kr.devport.api.domain.port.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCommentService {

    private final ProjectCommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectCommentVoteRepository voteRepository;

    @Transactional(readOnly = true)
    public List<ProjectCommentResponse> getCommentsByProject(String projectExternalId, Long currentUserId) {
        List<ProjectComment> comments = commentRepository.findAllByProjectExternalId(projectExternalId);
        return comments.stream()
            .map(c -> toCommentResponse(c, currentUserId))
            .collect(Collectors.toList());
    }

    @Transactional
    public ProjectCommentResponse createComment(
        String projectExternalId,
        CommentCreateRequest request,
        Long userId
    ) {
        Project project = projectRepository.findByExternalId(projectExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectExternalId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        ProjectComment comment = ProjectComment.builder()
            .project(project)
            .user(user)
            .content(request.getContent())
            .build();

        if (request.getParentCommentId() != null) {
            ProjectComment parent = commentRepository.findByExternalId(request.getParentCommentId())
                .orElseThrow(() -> new IllegalArgumentException("Parent comment not found: " + request.getParentCommentId()));

            if (!parent.getProject().getId().equals(project.getId())) {
                throw new IllegalArgumentException("Parent comment does not belong to this project");
            }
            comment.setParentComment(parent);
        }

        ProjectComment saved = commentRepository.save(comment);
        log.info("Created comment {} for project {}", saved.getExternalId(), projectExternalId);
        return toCommentResponse(saved, userId);
    }

    @Transactional
    public ProjectCommentResponse updateComment(String commentExternalId, CommentUpdateRequest request, Long userId) {
        ProjectComment comment = commentRepository.findByExternalId(commentExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentExternalId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only edit your own comments");
        }

        if (comment.getDeleted()) {
            throw new IllegalArgumentException("Cannot edit a deleted comment");
        }

        comment.setContent(request.getContent());
        comment.setUpdatedAt(LocalDateTime.now());
        ProjectComment updated = commentRepository.save(comment);
        log.info("Updated comment {}", commentExternalId);
        return toCommentResponse(updated, userId);
    }

    @Transactional
    public void deleteComment(String commentExternalId, Long userId) {
        ProjectComment comment = commentRepository.findByExternalId(commentExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentExternalId));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own comments");
        }

        comment.setDeleted(true);
        comment.setUpdatedAt(LocalDateTime.now());
        commentRepository.save(comment);
        log.info("Deleted comment {}", commentExternalId);
    }

    @Transactional
    public VoteResponse voteOnComment(String commentExternalId, VoteRequest request, Long userId) {
        ProjectComment comment = commentRepository.findByExternalId(commentExternalId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<ProjectCommentVote> existingVote =
            voteRepository.findByComment_ExternalIdAndUser_Id(commentExternalId, userId);

        if (request.getVote() == 0) {
            // Remove vote
            existingVote.ifPresent(vote -> {
                voteRepository.delete(vote);
                updateCommentVoteScore(comment);
                log.info("Removed vote on comment {}", commentExternalId);
            });
        } else if (existingVote.isPresent()) {
            ProjectCommentVote vote = existingVote.get();
            if (vote.getVote().equals(request.getVote().shortValue())) {
                // Same vote = toggle off (remove)
                voteRepository.delete(vote);
                log.info("Toggled off vote on comment {}", commentExternalId);
            } else {
                // Change vote direction
                vote.setVote(request.getVote().shortValue());
                voteRepository.save(vote);
                log.info("Changed vote on comment {}", commentExternalId);
            }
            updateCommentVoteScore(comment);
        } else {
            // New vote
            ProjectCommentVote vote = ProjectCommentVote.builder()
                .comment(comment)
                .user(user)
                .vote(request.getVote().shortValue())
                .build();
            voteRepository.save(vote);
            updateCommentVoteScore(comment);
            log.info("Added new vote on comment {}", commentExternalId);
        }

        Integer score = voteRepository.calculateVoteScore(commentExternalId);
        return VoteResponse.builder()
            .votes(score != null ? score : 0)
            .userVote(request.getVote())
            .build();
    }

    private void updateCommentVoteScore(ProjectComment comment) {
        Integer score = voteRepository.calculateVoteScore(comment.getExternalId());
        comment.setVoteScore(score != null ? score : 0);
        commentRepository.save(comment);
    }

    private ProjectCommentResponse toCommentResponse(ProjectComment comment, Long currentUserId) {
        Integer userVote = 0;
        if (currentUserId != null) {
            Optional<ProjectCommentVote> vote = voteRepository.findByComment_ExternalIdAndUser_Id(
                comment.getExternalId(), currentUserId
            );
            userVote = vote.map(v -> (int) v.getVote()).orElse(0);
        }

        return ProjectCommentResponse.builder()
            .id(comment.getExternalId())
            .content(comment.getContent())
            .deleted(comment.getDeleted())
            .parentId(comment.getParentComment() != null ? comment.getParentComment().getExternalId() : null)
            .author(CommentAuthorResponse.from(comment.getUser()))
            .votes(comment.getVoteScore())
            .userVote(userVote)
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .isOwner(currentUserId != null && comment.getUser().getId().equals(currentUserId))
            .build();
    }
}
