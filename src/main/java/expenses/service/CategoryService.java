package expenses.service;

import expenses.jooq.generated.tables.records.CategoriesRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static expenses.bot.NormalizationHelper.normalize;
import static expenses.jooq.generated.Tables.CATEGORIES;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private DSLContext dsl;

    public CategoriesRecord upsertCategory(Long userId, String name) {
        String normalizedName = normalize(name);

        return dsl.insertInto(CATEGORIES)
                .set(CATEGORIES.USER_ID, userId)
                .set(CATEGORIES.NAME, normalizedName)
                .onConflict(CATEGORIES.USER_ID, CATEGORIES.NAME)
                .doUpdate()
                .set(CATEGORIES.UPDATED_AT, Instant.now())
                .returning()
                .fetchOne();
    }

    public String listCategoriesAsString(Long userId) {
        var categories = dsl.select(CATEGORIES.NAME)
                .from(CATEGORIES)
                .where(CATEGORIES.USER_ID.eq(userId))
                .orderBy(CATEGORIES.NAME.asc())
                .fetchInto(String.class);

        return String.join(", ", categories);
    }
}
