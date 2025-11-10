package expenses.dto.v2;

public record ExpenseAction(
        Action action,
        Transaction transaction,
        Update update,
        String reason
) {
    public enum Action {
        CREATE_TRANSACTION,
        UPDATE_TRANSACTION,
        CLARIFY,
        IGNORE
    }
}