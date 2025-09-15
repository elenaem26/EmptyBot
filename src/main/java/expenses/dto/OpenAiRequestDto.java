package expenses.dto;

import java.util.Collection;

public record OpenAiRequestDto(
        String userMessage,
        Collection<String> categories
) {
}
