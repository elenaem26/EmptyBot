package expenses.service;

import expenses.dto.ExpensesAndCategoriesRecord;
import expenses.dto.OpenAiExpenseDto;
import expenses.dto.OpenAiExpensesResponseDto;
import expenses.jooq.generated.tables.records.CategoriesRecord;
import expenses.jooq.generated.tables.records.ExpensesRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String DEFAULT_CURRENCY = "GEL";

    public ExpensesAndCategoriesRecord createExpensesAndCategories(OpenAiExpensesResponseDto expensesDto) {
        Set<String> categories = expensesDto.expenses().stream().map(OpenAiExpenseDto::category).collect(Collectors.toSet());
        List<CategoriesRecord> categoriesSaved = categoryService.createCategoriesIfNotExists(categories);
        Map<String, UUID> categoryIdByName = categoriesSaved.stream()
                .collect(Collectors.toMap(CategoriesRecord::getName, CategoriesRecord::getId));

        List<ExpensesRecord> expensesToSave = new ArrayList<>();
        for (OpenAiExpenseDto expenseDto : expensesDto.expenses()) {
            UUID categoryId = categoryIdByName.get(expenseDto.category());
            expensesToSave.add(mapExpense(expenseDto, categoryId));
        }
        dsl.batchInsert(expensesToSave).execute();

        return new ExpensesAndCategoriesRecord(expensesToSave, categoriesSaved);
    }

    public ExpensesRecord createExpensesAndCategories(OpenAiExpenseDto expenseDto, CategoriesRecord category) {
        var expense = mapExpense(expenseDto, category.getId());
        dsl.executeInsert(expense);
        return expense;
    }

    private ExpensesRecord mapExpense(OpenAiExpenseDto dto, UUID categoryId) {
        UUID id = UUID.randomUUID();
        ExpensesRecord r = dsl.newRecord(EXPENSES);
        r.setId(id);
        r.setName(dto.name());
        r.setPrice(dto.price());
        r.setCategoryId(categoryId);
        if (dto.currency() == null || dto.currency().isBlank()) {
            r.setCurrency(DEFAULT_CURRENCY);
        } else {
            r.setCurrency(dto.currency());
        }
        return r;
    }

    public void deleteExpense(UUID id) {
        dsl.deleteFrom(EXPENSES)
                .where(EXPENSES.ID.eq(id))
                .execute();
    }


    public List<ExpensesRecord> find() {
        return dsl.selectFrom(EXPENSES).fetch();
    }
}

