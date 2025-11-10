package expenses.configuration;

import expenses.bot.ExpensesBot;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class BotConfiguration {

    @Value("classpath:prompts/SystemPrompt.txt")
    private Resource promptResource;

    @Value("classpath:schema/expense-schema.json")
    private Resource schemaJson;

    @Bean
    public TelegramBotsApi init (ExpensesBot expensesBot) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(expensesBot);
            return telegramBotsApi;
        } catch (TelegramApiException ex) {
            throw new RuntimeException("Error registering bot: ${e.message}", ex);
        }
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        try (var in = promptResource.getInputStream()) {
            String systemPrompt = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            return builder
                    .defaultSystem(systemPrompt)
                    .defaultOptions(
                            OpenAiChatOptions.builder()
                                    .model("gpt-4o-mini")
                                    .temperature(0.0)
                                    .build()
                    )
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read SystemPrompt.txt", e);
        }
    }

    @Bean
    public String expenseJsonSchema() {
        try (var in = schemaJson.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read schema file: schema/expense-schema.json", e);
        }
    }
}
