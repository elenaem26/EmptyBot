package expenses.service;

import expenses.dto.v2.Update;
import expenses.jooq.generated.tables.records.CategoryRulesRecord;
import expenses.jooq.generated.tables.records.ExpensesRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static expenses.bot.NormalizationHelper.normalize;
import static expenses.jooq.generated.Tables.CATEGORY_RULES;

@Service
@Transactional
public class CategoryRuleService {

    @Autowired
    private DSLContext dsl;

    public Optional<String> findCategory(Long userId, String keywordSource) {
        if (userId == null || keywordSource == null || keywordSource.isBlank()) {
            return Optional.empty();
        }

        String keyword = normalize(keywordSource);

        return dsl.select(CATEGORY_RULES.CATEGORY)
                .from(CATEGORY_RULES)
                .where(CATEGORY_RULES.USER_ID.eq(userId))
                .and(CATEGORY_RULES.KEYWORD.eq(keyword))
                .fetchOptional(CATEGORY_RULES.CATEGORY);
    }

    public Optional<CategoryRulesRecord> upsertRule(Long userId, ExpensesRecord baseExpense, Update update) {
        if (update == null || update.category() == null) {
            return Optional.empty();
        }

        String titleSource = update.title() == null ? baseExpense.getName() : update.title();
        if (titleSource == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(upsertRule(userId, titleSource, update.category()));
    }

    public String listRulesAsString(Long userId) {
        var rules = dsl.select(CATEGORY_RULES.KEYWORD, CATEGORY_RULES.CATEGORY)
                .from(CATEGORY_RULES)
                .where(CATEGORY_RULES.USER_ID.eq(userId))
                .orderBy(CATEGORY_RULES.KEYWORD.asc())
                .fetch();

        return rules.stream()
                .map(r -> r.get(CATEGORY_RULES.KEYWORD) + " → " + r.get(CATEGORY_RULES.CATEGORY))
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private CategoryRulesRecord upsertRule(Long userId, String keywordSource, String category) {
        if (userId == null || keywordSource == null || category == null) {
            throw new IllegalArgumentException("userId, keyword and category must be non-null");
        }

        String keyword = normalize(keywordSource);
        Instant now = Instant.now();

        return dsl.insertInto(CATEGORY_RULES)
                .set(CATEGORY_RULES.USER_ID, userId)
                .set(CATEGORY_RULES.KEYWORD, keyword)
                .set(CATEGORY_RULES.CATEGORY, category)
                .onConflict(CATEGORY_RULES.USER_ID, CATEGORY_RULES.KEYWORD)
                .doUpdate()
                .set(CATEGORY_RULES.CATEGORY, category)
                .set(CATEGORY_RULES.UPDATED_AT, now)
                .returning()
                .fetchOne();
    }
}
