package com.example.emptybot.service;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MyTelegramBot extends AbilityBot {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private ChatClient chatClient;

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
    public void handleAiPhoto(BaseAbilityBot bot, Update update) {
        Long chatId = update.getMessage().getChatId();
        try {
            // 1) извлечь fileId (photo или document с image/*)
            String fileId = null;
            MimeType mime = null;

            if (update.getMessage() != null && update.getMessage().hasPhoto()) {
                List<PhotoSize> photos = update.getMessage().getPhoto();
                PhotoSize best = photos.stream()
                        .max(Comparator.comparingInt(p -> p.getWidth() * p.getHeight()))
                        .orElseThrow();
                fileId = best.getFileId();
                mime = MimeTypeUtils.IMAGE_JPEG; // телега фотки обычно в jpeg
            } else if (update.getMessage() != null && update.getMessage().hasDocument()
                    && update.getMessage().getDocument().getMimeType() != null
                    && update.getMessage().getDocument().getMimeType().startsWith("image/")) {
                fileId = update.getMessage().getDocument().getFileId();
                mime = MimeType.valueOf(update.getMessage().getDocument().getMimeType());
            } else {
                silent.send("Пришли картинку (photo/document).", chatId);
                return;
            }

            // 2) получить FilePath и скачать файл
            var tgFile = execute(new GetFile(fileId));                 // API getFile
            var localFile = downloadFile(tgFile);                       // скачивает во временный файл
            byte[] bytes = Files.readAllBytes(localFile.toPath());

            String answer = ocrService.ocrBytes(bytes);
            String response = chatClient.prompt()
                    .user("Here is OCR text from a receipt: \n" + answer)
                    .call()
                    .content();

            // 4) ответ в чат
            silent.send(answer, chatId);
            silent.send(response, chatId);

        } catch (Exception e) {
            silent.send("Не удалось обработать изображение: " + e.getMessage(), chatId);
        }
    }

    public Ability ai() {
        return Ability
                .builder()
                .name("ai")
                .info("Отправить текст в OpenAI")
                .locality(org.telegram.abilitybots.api.objects.Locality.ALL)
                .privacy(org.telegram.abilitybots.api.objects.Privacy.PUBLIC)
                .action(this::handleAiCommand)
                .build();
    }

    private void handleAiCommand(MessageContext ctx) {
        String prompt = String.join(" ", ctx.arguments());
        String response = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        silent.send(response, ctx.chatId());
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

    public Reply handlePhotos() {
        return Reply.of(this::handleAiPhoto,
                Flag.PHOTO);
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

    private void replyPhoto(BaseAbilityBot bot, Update update) {
        Long chatId = update.getMessage().getChatId();
        silent.send("фото", chatId);
    }

    private String getCompanyName(String text, String command) {
        return text.trim().substring(command.length()).trim();
    }


    @Override
    public List<Reply> replies() {
        return List.of(handlePhotos(), handleMessages(), handleCallbacks());
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

