package expenses.dto.v2;

public record Update(
        Target target,
        String title,
        Double amount,
        String currency,
        String category
) {
    public enum Target {
        LAST_TRANSACTION, BY_TITLE
    }
}