package expenses.dto;

import java.util.List;

public record OpenAiRequestDto(
        String userMessage,
        List<String> categories
) {
}
