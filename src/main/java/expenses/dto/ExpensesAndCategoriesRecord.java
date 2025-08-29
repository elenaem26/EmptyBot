package expenses.dto;

import expenses.jooq.generated.tables.records.CategoriesRecord;
import expenses.jooq.generated.tables.records.ExpensesRecord;

import java.util.List;

public record ExpensesAndCategoriesRecord(
        List<ExpensesRecord> expenses,
        List<CategoriesRecord> categories
) {
}
