package expenses.dto.v2;

public record Transaction(
        String title,
        Double amount,
        String currency,
        String category
) {}