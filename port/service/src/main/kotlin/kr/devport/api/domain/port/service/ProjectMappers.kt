package kr.devport.api.domain.port.service

import kr.devport.api.domain.auth.UserSummary
import kr.devport.api.domain.port.dto.response.ProjectCommentAuthorResponse
import kr.devport.api.domain.port.dto.response.ProjectCommentResponse
import kr.devport.api.domain.port.dto.response.ProjectDetailResponse
import kr.devport.api.domain.port.dto.response.ProjectEventResponse
import kr.devport.api.domain.port.entity.Project
import kr.devport.api.domain.port.entity.ProjectComment
import kr.devport.api.domain.port.entity.ProjectEvent

internal fun Project.toDetailResponse(): ProjectDetailResponse =
    ProjectDetailResponse(
        id = externalId,
        name = name,
        fullName = fullName,
        repoUrl = repoUrl,
        homepageUrl = homepageUrl,
        description = description,
        stars = stars,
        forks = forks,
        contributors = contributors,
        language = language,
        languageColor = languageColor,
        license = license,
        lastRelease = lastRelease,
        tags = tags,
    )

internal fun ProjectEvent.toEventResponse(): ProjectEventResponse =
    ProjectEventResponse(
        id = externalId,
        version = version,
        releasedAt = releasedAt,
        eventTypes = eventTypes,
        summary = summary,
        bullets = bullets,
        impactScore = impactScore,
        isSecurity = isSecurity,
        isBreaking = isBreaking,
        sourceUrl = sourceUrl,
    )

internal fun ProjectComment.toCommentResponse(
    author: UserSummary?,
    currentUserId: Long?,
    userVote: Int,
): ProjectCommentResponse =
    ProjectCommentResponse(
        id = externalId,
        content = content,
        deleted = deleted,
        parentId = parentComment?.externalId,
        author = ProjectCommentAuthorResponse.from(author),
        votes = voteScore,
        userVote = userVote,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isOwner = currentUserId != null && userId == currentUserId,
    )
