package com.example.emptybot.configuration;

import com.example.emptybot.service.MyTelegramBot;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfiguration {

    @Bean
    public TelegramBotsApi init (MyTelegramBot myTelegramBot) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(myTelegramBot);
            return telegramBotsApi;
        } catch (TelegramApiException ex) {
            throw new RuntimeException("Error registering bot: ${e.message}", ex);
        }
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(ChatOptions.builder().model("gpt-4o-mini").temperature(0.0).build())
                .defaultSystem("""
                        You are a receipt parsing assistant.\s
                        You receive OCR text extracted from a receipt (in Georgian and/or English).
                        Your job is to normalize it and return structured JSON only.
                        
                        Rules:
                        - Output strictly valid JSON, no extra text.
                        - JSON schema:\s
                        {
                          "items": [
                            {"name_en": "string or 'unknown'", "name_ka": "string or 'unknown'", "price": number},
                            ...
                          ],
                          "date": "YYYY-MM-DD or 'unknown'"
                        }
                        - If the product has both Georgian and English names, keep both.
                        - If there is only Georgian, set English to "unknown".
                        - If there is only English, set Georgian to "unknown".
                        - If unsure in any name, set it to "unknown".
                        - Extract price as a number (no currency symbol).
                        - Date: detect if present, format YYYY-MM-DD, else "unknown".
                        - Do not invent values.
                        """)
                .build();
    }
}
