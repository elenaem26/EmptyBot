package expenses.dto;

import java.math.BigDecimal;

public record OpenAiExpenseDto(
        String name,
        String description,
        BigDecimal price,
        Integer amount,
        String currency,
        String category
) {
}
