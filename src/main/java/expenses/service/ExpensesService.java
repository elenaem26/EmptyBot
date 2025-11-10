package expenses.service;

import expenses.dto.ExpensesAndCategoriesRecord;
import expenses.dto.OpenAiExpenseDto;
import expenses.dto.OpenAiExpensesResponseDto;
import expenses.dto.v2.Transaction;
import expenses.jooq.generated.tables.records.CategoriesRecord;
import expenses.jooq.generated.tables.records.ExpensesRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static expenses.jooq.generated.Tables.EXPENSES;

@Service
@Transactional
public class ExpensesService {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private CategoryService categoryService;


    public ExpensesRecord insertExpense(Long userId, Transaction tx) {
        if (tx == null || tx.title() == null) {
            throw new IllegalArgumentException("Transaction must have a title");
        }

        var category = categoryService.upsertCategory(userId, tx.category());
        Long categoryId = category.getId();

        return dsl.insertInto(EXPENSES)
                .set(EXPENSES.USER_ID, userId)
                .set(EXPENSES.NAME, tx.title().trim())
                .set(EXPENSES.PRICE, BigDecimal.valueOf(tx.amount() != null ? tx.amount() : 0))
                .set(EXPENSES.CURRENCY, tx.currency() != null ? tx.currency() : "UNKNOWN")
                .set(EXPENSES.CATEGORY_ID, categoryId)
                .returning()
                .fetchOne();
    }

//    public ExpensesAndCategoriesRecord createExpensesAndCategories(OpenAiExpensesResponseDto expensesDto) {
//        Set<String> categories = expensesDto.expenses().stream().map(OpenAiExpenseDto::category).collect(Collectors.toSet());
//        List<CategoriesRecord> categoriesSaved = categoryService.createCategoriesIfNotExists(categories);
//        Map<String, UUID> categoryIdByName = categoriesSaved.stream()
//                .collect(Collectors.toMap(CategoriesRecord::getName, CategoriesRecord::getId));
//
//        List<ExpensesRecord> expensesToSave = new ArrayList<>();
//        for (OpenAiExpenseDto expenseDto : expensesDto.expenses()) {
//            UUID categoryId = categoryIdByName.get(expenseDto.category());
//            expensesToSave.add(mapExpense(expenseDto, categoryId));
//        }
//        dsl.batchInsert(expensesToSave).execute();
//
//        return new ExpensesAndCategoriesRecord(expensesToSave, categoriesSaved);
//    }
//
//    public ExpensesRecord createExpensesAndCategories(OpenAiExpenseDto expenseDto, CategoriesRecord category) {
//        var expense = mapExpense(expenseDto, category.getId());
//        dsl.executeInsert(expense);
//        return expense;
//    }
//
//    private ExpensesRecord mapExpense(OpenAiExpenseDto dto, UUID categoryId) {
//        UUID id = UUID.randomUUID();
//        ExpensesRecord r = dsl.newRecord(EXPENSES);
//        r.setId(id);
//        r.setName(dto.name());
//        r.setPrice(dto.price());
//        r.setCategoryId(categoryId);
//        if (dto.currency() == null || dto.currency().isBlank()) {
//            r.setCurrency(DEFAULT_CURRENCY);
//        } else {
//            r.setCurrency(dto.currency());
//        }
//        return r;
//    }
//
//    public void deleteExpense(UUID id) {
//        dsl.deleteFrom(EXPENSES)
//                .where(EXPENSES.ID.eq(id))
//                .execute();
//    }
//
//
//    public List<ExpensesRecord> find() {
//        return dsl.selectFrom(EXPENSES).fetch();
//    }
}

