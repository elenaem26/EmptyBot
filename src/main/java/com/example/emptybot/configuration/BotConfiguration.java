package com.example.emptybot.configuration;

import com.example.emptybot.service.MyTelegramBot;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
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
        try (var in = promptResource.getInputStream()) {
            return builder
                    .defaultOptions(ChatOptions.builder().model("gpt-4o-mini").temperature(0.0).build())
                    .defaultSystem(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Cannot read SystemPrompt.txt", e);
        }
    }
}
