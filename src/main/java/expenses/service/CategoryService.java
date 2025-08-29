package expenses.service;

import expenses.jooq.generated.tables.records.CategoriesRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static expenses.jooq.generated.Tables.CATEGORIES;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private DSLContext dsl;

    public List<CategoriesRecord> createCategoriesIfNotExists(Set<String> categories) {
        Map<String, CategoriesRecord> categoriesByName = dsl.selectFrom(CATEGORIES).forUpdate().fetch()
                .collect(Collectors.toMap(CategoriesRecord::getName, c -> c));

        List<CategoriesRecord> recordsToSave = new ArrayList<>();
        for (String category : categories) {
            if (!categoriesByName.containsKey(category)) {
                CategoriesRecord newCategory = mapCategory(category);
                recordsToSave.add(newCategory);
                categoriesByName.put(newCategory.getName(), newCategory);
            }
        }
        dsl.batchInsert(recordsToSave).execute();
        return categoriesByName.values().stream().toList();
    }

    public CategoriesRecord mapCategory(String name) {
        UUID id = UUID.randomUUID();
        CategoriesRecord r = dsl.newRecord(CATEGORIES);
        r.setId(id);
        r.setName(name);
        return r;
    }

    public List<CategoriesRecord> find() {
        return dsl.selectFrom(CATEGORIES).fetch();
    }
}
