package com.example.emptybot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
public class MyTelegramBot extends AbilityBot {

    @Autowired
    private CompanyService companyService;

    public MyTelegramBot(Environment env) {
        super(env.getProperty("bottoken"), env.getProperty("botname"));
    }

    public Ability start() {
        return Ability.builder()
                .name("start")
                .info("Приветствие")
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                    long chatId = ctx.chatId();
                    SendMessage msg = SendMessage.builder()
                            .chatId(Long.toString(chatId))
                            .text("Привет! Выберите действие:")
                            .replyMarkup(inlineKeyboard())
                            .build();
                    silent.execute(msg);
                })
                .build();
    }

    public Ability menu() {
        return Ability.builder()
                .name("menu")
                .info("Показать меню")
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                    long chatId = ctx.chatId();
                    SendMessage msg = SendMessage.builder()
                            .chatId(Long.toString(chatId))
                            .text("Меню открыто. Нажимай кнопки снизу.")
                            .replyMarkup(replyKeyboard())     // обычная клавиатура
                            .build();
                    silent.execute(msg);
                })
                .build();
    }


    public Reply handleCallbacks() {
        return Reply.of((bot, update) -> {
            CallbackQuery cq = update.getCallbackQuery();
            String data = cq.getData();
            long chatId = cq.getMessage().getChatId();

            switch (data) {
                case "ACTION_A" -> silent.send("Вы выбрали A ✅", chatId);
                case "ACTION_B" -> silent.send("Вы выбрали B ✅", chatId);
                default -> silent.send("Неизвестное действие 🤔", chatId);
            }

            if (cq.getMessage() instanceof Message) {
                EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                        .chatId(cq.getMessage().getChatId().toString())
                        .messageId(((Message) cq.getMessage()).getMessageId())
                        .replyMarkup(null)
                        .build();
                silent.execute(edit);
            }
        }, Flag.CALLBACK_QUERY);
    }

    public Reply handleMessages() {
        return Reply.of(this::reply,
                Flag.TEXT,
                update -> !update.getMessage().getText().startsWith("/"));
    }

    private void reply(BaseAbilityBot bot, Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if (text.toLowerCase().startsWith("save")) {
            String company = getCompanyName(text, "save");
            companyService.saveCompany(company);
            silent.send("Компания " + company + " сохранена", chatId);
        } else if (text.toLowerCase().startsWith("delete")) {
            String company = getCompanyName(text, "delete");
            companyService.deleteCompany(company);
            silent.send("Компания " + company + " удалена", chatId);
        } else {
            silent.send("Команда " + text + " не распознана", chatId);
        }
    }

    private String getCompanyName(String text, String command) {
        return text.trim().substring(command.length()).trim();
    }


    @Override
    public List<Reply> replies() {
        return List.of(handleMessages(), handleCallbacks());
    }

    private ReplyKeyboardMarkup replyKeyboard() {
        KeyboardRow row1 = new KeyboardRow(List.of(
                KeyboardButton.builder().text("Помощь").build(),
                KeyboardButton.builder().text("О боте").build()
        ));
        KeyboardRow row2 = new KeyboardRow(List.of(
                KeyboardButton.builder().text("Поделиться контактом").requestContact(true).build()
        ));
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .resizeKeyboard(true)
                .oneTimeKeyboard(true)
                .build();
    }

    private InlineKeyboardMarkup inlineKeyboard() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(
                InlineKeyboardButton.builder().text("Кнопка A").callbackData("ACTION_A").build(),
                InlineKeyboardButton.builder().text("Кнопка B").callbackData("ACTION_B").build()
        ));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    @Override
    public long creatorId() {
        return 1L;
    }
}

