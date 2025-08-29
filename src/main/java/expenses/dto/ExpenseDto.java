package expenses.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseDto(
        String name,
        String description,
        BigDecimal price,
        Integer amount,
        String currency,
        UUID categoryId
) {
}
