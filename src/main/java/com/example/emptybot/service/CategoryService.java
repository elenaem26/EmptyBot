package com.example.emptybot.service;

import com.example.jooq.generated.tables.records.CategoriesRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.example.jooq.generated.Tables.CATEGORIES;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private DSLContext dsl;

    public UUID create(String name) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(CATEGORIES).set(CATEGORIES.ID, id).set(CATEGORIES.NAME, name).execute();
        return id;
    }

    public List<CategoriesRecord> find() {
        return dsl.selectFrom(CATEGORIES).fetch();
    }
}
