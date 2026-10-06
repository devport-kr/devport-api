package kr.devport.api.domain.newsletter.service;

import kr.devport.api.domain.article.entity.Article;
import kr.devport.api.domain.article.entity.ArticleMetadata;
import kr.devport.api.domain.article.enums.ItemType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 주간 다이제스트에 넣을 글을 고른다.
 * <ul>
 *   <li>Article.score는 크롤러가 매일 시간 감쇠를 다시 적용한 값이라, 같은 주 안에서도 월요일 글이 토요일 글보다 불리하다.
 *       그래서 감쇠를 뺀 참여도(추천 × 댓글 가중치)로 비교한다.</li>
 *   <li>HN 포인트와 dev.to 반응 수는 규모가 달라서(HN은 50점 이상만 수집) 그대로 비교하면 HN이 매주 독식한다.
 *       item type(DISCUSSION=HN, BLOG=dev.to) 안에서의 백분위로 비교하고, 한 종류는 최대 {@code maxPerType}개까지만 고른다.
 *       HN 글의 source는 원문 도메인이라 source로는 묶을 수 없다.</li>
 * </ul>
 */
final class WeeklyDigestPicker {

    private WeeklyDigestPicker() {
    }

    private record Ranked(Article article, double engagement, double percentile) {
    }

    static List<Article> pick(List<Article> candidates, int limit, int maxPerType) {
        Map<ItemType, List<Article>> byType = new EnumMap<>(ItemType.class);
        for (Article article : candidates) {
            if (engagement(article) > 0) {
                byType.computeIfAbsent(article.getItemType(), type -> new ArrayList<>()).add(article);
            }
        }

        List<Ranked> ranked = new ArrayList<>();
        for (List<Article> group : byType.values()) {
            group.sort(Comparator.comparingDouble(WeeklyDigestPicker::engagement).reversed()
                .thenComparing(Article::getId));
            for (int i = 0; i < group.size(); i++) {
                Article article = group.get(i);
                ranked.add(new Ranked(article, engagement(article), 1.0 - (double) i / group.size()));
            }
        }
        ranked.sort(Comparator.comparingDouble(Ranked::percentile).reversed()
            .thenComparing(Comparator.comparingDouble(Ranked::engagement).reversed())
            .thenComparing(r -> r.article().getId()));

        List<Article> picked = new ArrayList<>(limit);
        Map<ItemType, Integer> perType = new EnumMap<>(ItemType.class);
        for (Ranked r : ranked) {
            if (picked.size() == limit) {
                break;
            }
            ItemType type = r.article().getItemType();
            if (perType.getOrDefault(type, 0) < maxPerType) {
                picked.add(r.article());
                perType.merge(type, 1, Integer::sum);
            }
        }
        return picked;
    }

    /** 크롤러 ScorerService와 같은 식에서 시간 감쇠와 source 가중치만 뺀 값 */
    static double engagement(Article article) {
        return votes(article) * commentMultiplier(comments(article));
    }

    /** HN 포인트 / dev.to 반응 수 (REPO는 스타) */
    static int votes(Article article) {
        ArticleMetadata metadata = article.getMetadata();
        return metadata == null ? 0 : Math.max(orZero(metadata.getUpvotes()), orZero(metadata.getStars()));
    }

    static int comments(Article article) {
        ArticleMetadata metadata = article.getMetadata();
        return metadata == null ? 0 : orZero(metadata.getComments());
    }

    private static double commentMultiplier(int comments) {
        if (comments == 0) return 1.0;
        if (comments < 5) return 1.1;
        if (comments < 10) return 1.2;
        if (comments < 20) return 1.3;
        if (comments < 50) return 1.4;
        return 1.5;
    }

    private static int orZero(Integer value) {
        return value != null ? value : 0;
    }
}
