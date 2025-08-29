package expenses.dto;

import java.util.List;

public record OpenAiExpensesResponseDto(
        List<OpenAiExpenseDto> expenses
) {
}

