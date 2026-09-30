package kr.devport.api.domain.llm.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.devport.api.domain.llm.entity.LLMModel;
import kr.devport.api.domain.llm.entity.QLLMModel;
import kr.devport.api.domain.llm.entity.QModelCreator;
import kr.devport.api.domain.llm.dto.request.LLMModelSearchCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Repository
@RequiredArgsConstructor
public class LLMModelRepositoryImpl implements LLMModelRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QLLMModel model = QLLMModel.lLMModel;
    private static final QModelCreator creator = QModelCreator.modelCreator;

    @Override
    public Page<LLMModel> searchWithCondition(LLMModelSearchCondition condition, Pageable pageable) {
        List<LLMModel> content = queryFactory
            .selectFrom(model)
            .leftJoin(model.modelCreator, creator).fetchJoin()
            .where(
                providerEq(condition.getProvider()),
                creatorSlugEq(condition.getCreatorSlug()),
                licenseEq(condition.getLicense()),
                priceLoe(condition.getMaxPrice()),
                contextWindowGoe(condition.getMinContextWindow()),
                keywordContains(condition.getKeyword()),
                releaseDateGoe(condition.getReleaseDateFrom()),
                releaseDateLoe(condition.getReleaseDateTo()),
                intelligenceScoreGoe(condition.getMinScore())
            )
            .orderBy(model.scoreAaIntelligenceIndex.desc().nullsLast())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(model.count())
            .from(model)
            .leftJoin(model.modelCreator, creator)
            .where(
                providerEq(condition.getProvider()),
                creatorSlugEq(condition.getCreatorSlug()),
                licenseEq(condition.getLicense()),
                priceLoe(condition.getMaxPrice()),
                contextWindowGoe(condition.getMinContextWindow()),
                keywordContains(condition.getKeyword()),
                releaseDateGoe(condition.getReleaseDateFrom()),
                releaseDateLoe(condition.getReleaseDateTo()),
                intelligenceScoreGoe(condition.getMinScore())
            )
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    @Override
    public List<LLMModel> findAllWithCondition(LLMModelSearchCondition condition) {
        return queryFactory
            .selectFrom(model)
            .leftJoin(model.modelCreator, creator).fetchJoin()
            .where(
                providerEq(condition.getProvider()),
                creatorSlugEq(condition.getCreatorSlug()),
                licenseEq(condition.getLicense()),
                priceLoe(condition.getMaxPrice()),
                contextWindowGoe(condition.getMinContextWindow()),
                keywordContains(condition.getKeyword()),
                releaseDateGoe(condition.getReleaseDateFrom()),
                releaseDateLoe(condition.getReleaseDateTo()),
                intelligenceScoreGoe(condition.getMinScore())
            )
            .orderBy(model.scoreAaIntelligenceIndex.desc().nullsLast())
            .fetch();
    }

    // ========== BooleanExpression Methods (Type-safe condition building) ==========

    private BooleanExpression providerEq(String provider) {
        return hasText(provider) ? model.provider.eq(provider) : null;
    }

    private BooleanExpression creatorSlugEq(String creatorSlug) {
        return hasText(creatorSlug) ? creator.slug.eq(creatorSlug) : null;
    }

    private BooleanExpression licenseEq(String license) {
        return hasText(license) ? model.license.eq(license) : null;
    }

    private BooleanExpression priceLoe(BigDecimal maxPrice) {
        return maxPrice != null ? model.priceBlended.loe(maxPrice) : null;
    }

    private BooleanExpression contextWindowGoe(Long minContextWindow) {
        return minContextWindow != null ? model.contextWindow.goe(minContextWindow) : null;
    }

    private BooleanExpression keywordContains(String keyword) {
        return hasText(keyword) ? model.modelName.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression releaseDateGoe(LocalDate from) {
        return from != null ? model.releaseDate.goe(from) : null;
    }

    private BooleanExpression releaseDateLoe(LocalDate to) {
        return to != null ? model.releaseDate.loe(to) : null;
    }

    private BooleanExpression intelligenceScoreGoe(BigDecimal minScore) {
        return minScore != null ? model.scoreAaIntelligenceIndex.goe(minScore) : null;
    }
}
