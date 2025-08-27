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
                .defaultSystem("You are a strict information extractor. Input language: Georgian (ka).\n" +
                        "Do not guess. If unsure, use \"unknown\".")
                .build();
    }
}
