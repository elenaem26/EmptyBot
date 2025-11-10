package expenses.bot;

import expenses.dto.v2.ExpenseAction;
import expenses.dto.v2.Transaction;
import expenses.dto.v2.Update;

import java.text.DecimalFormat;

public class ExpenseFormatter {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.##");

    public static String formatResponse(ExpenseAction action) {
        if (action == null || action.action() == null) {
            return "Не удалось обработать сообщение 😕";
        }

        return switch (action.action()) {
            case CREATE_TRANSACTION -> formatCreate(action.transaction());
            case UPDATE_TRANSACTION -> formatUpdate(action.update());
            case CLARIFY -> formatClarify(action.reason());
            case IGNORE -> "Сообщение не похоже на расход 🤷‍♀️";
        };
    }

    private static String formatCreate(Transaction tx) {
        if (tx == null) return "Не удалось создать запись о трате.";

        String title = tx.title() != null ? tx.title() : "(без названия)";
        String category = tx.category() != null ? tx.category() : "без категории";
        String currency = tx.currency() != null && !"UNKNOWN".equals(tx.currency()) ? tx.currency() : "";
        double amount = tx.amount() != null ? tx.amount() : 0.0;

        return String.format(
                "💾 Сохранил покупку **%s** на сумму %s %s в категорию **%s**.",
                title,
                PRICE_FORMAT.format(amount),
                currency,
                category
        );
    }

    private static String formatUpdate(Update update) {
        if (update == null) return "Изменений не найдено.";

        StringBuilder sb = new StringBuilder("🔄 Обновил последнюю покупку:");

        if (update.category() != null) {
            sb.append("\n• категория → **").append(update.category()).append("**");
        }
        if (update.amount() != null) {
            sb.append("\n• сумма → ").append(PRICE_FORMAT.format(update.amount()));
            if (update.currency() != null) sb.append(" ").append(update.currency());
        }
        if (update.currency() != null && update.amount() == null) {
            sb.append("\n• валюта → ").append(update.currency());
        }
        if (update.title() != null) {
            sb.append("\n• название → ").append(update.title());
        }

        if (sb.length() == "🔄 Обновил последнюю покупку:".length()) {
            sb.append("\n(без изменений)");
        }

        return sb.toString();
    }

    private static String formatClarify(String reason) {
        if (reason == null || reason.isBlank()) {
            return "❓ Не хватает информации, уточните пожалуйста.";
        }
        return "❓ Нужна ясность: " + reason;
    }
}

