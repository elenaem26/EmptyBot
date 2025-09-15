package expenses.dto;

import java.math.BigDecimal;

public record OpenAiExpenseDto(
        String name,
        BigDecimal price,
        String currency,
        String category,
        String suggestCategory
) {
}
