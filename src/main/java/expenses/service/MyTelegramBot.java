//package com.example.emptybot.service;
//
//import com.example.emptybot.dto.ReceiptDto;
//import com.example.jooq.generated.tables.records.CategoriesRecord;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.core.env.Environment;
//import org.springframework.stereotype.Service;
//import org.springframework.util.MimeType;
//import org.springframework.util.MimeTypeUtils;
//import org.telegram.abilitybots.api.bot.AbilityBot;
//import org.telegram.abilitybots.api.bot.BaseAbilityBot;
//import org.telegram.abilitybots.api.objects.*;
//import org.telegram.telegrambots.meta.api.methods.GetFile;
//import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
//import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
//import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
//import org.telegram.telegrambots.meta.api.objects.Message;
//import org.telegram.telegrambots.meta.api.objects.PhotoSize;
//import org.telegram.telegrambots.meta.api.objects.Update;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
//
//import java.nio.file.Files;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//public class MyTelegramBot extends AbilityBot {
//
//    private final Logger logger = LoggerFactory.getLogger(MyTelegramBot.class);
//
//    @Autowired
//    private CategoryService categoryService;
//
//    @Autowired
//    private PurchasesService purchasesService;
//
//    @Autowired
//    private ChatClient chatClient;
//
//    private ObjectMapper mapper = new ObjectMapper();
//
//    public MyTelegramBot(Environment env) {
//        super(env.getProperty("bottoken"), env.getProperty("botname"));
//    }
//
//    public Ability start() {
//        return Ability.builder()
//                .name("start")
//                .info("Приветствие")
//                .locality(Locality.ALL)
//                .privacy(Privacy.PUBLIC)
//                .action(ctx -> {
//                    long chatId = ctx.chatId();
//                    SendMessage msg = SendMessage.builder()
//                            .chatId(Long.toString(chatId))
//                            .text("Привет! Выберите действие:")
//                            .replyMarkup(inlineKeyboard())
//                            .build();
//                    silent.execute(msg);
//                })
//                .build();
//    }
//
//    public Ability menu() {
//        return Ability.builder()
//                .name("menu")
//                .info("Показать меню")
//                .locality(Locality.ALL)
//                .privacy(Privacy.PUBLIC)
//                .action(ctx -> {
//                    long chatId = ctx.chatId();
//                    SendMessage msg = SendMessage.builder()
//                            .chatId(Long.toString(chatId))
//                            .text("Меню открыто. Нажимай кнопки снизу.")
//                            .replyMarkup(replyKeyboard())     // обычная клавиатура
//                            .build();
//                    silent.execute(msg);
//                })
//                .build();
//    }
//
//    public void handleAiPhoto(BaseAbilityBot bot, Update update) {
//        Long chatId = update.getMessage().getChatId();
//        try {
//            // 1) извлечь fileId (photo или document с image/*)
//            String fileId = null;
//            MimeType mime = null;
//
//            if (update.getMessage() != null && update.getMessage().hasPhoto()) {
//                List<PhotoSize> photos = update.getMessage().getPhoto();
//                PhotoSize best = photos.stream()
//                        .max(Comparator.comparingInt(p -> p.getWidth() * p.getHeight()))
//                        .orElseThrow();
//                fileId = best.getFileId();
//                mime = MimeTypeUtils.IMAGE_JPEG; // телега фотки обычно в jpeg
//            } else if (update.getMessage() != null && update.getMessage().hasDocument()
//                    && update.getMessage().getDocument().getMimeType() != null
//                    && update.getMessage().getDocument().getMimeType().startsWith("image/")) {
//                fileId = update.getMessage().getDocument().getFileId();
//                mime = MimeType.valueOf(update.getMessage().getDocument().getMimeType());
//            } else {
//                silent.send("Пришли картинку (photo/document).", chatId);
//                return;
//            }
//
//            // 2) получить FilePath и скачать файл
//            var tgFile = execute(new GetFile(fileId));                 // API getFile
//            var localFile = downloadFile(tgFile);                       // скачивает во временный файл
//            byte[] bytes = Files.readAllBytes(localFile.toPath());
//
//            String answerGCV = ocrGCVService.read(bytes);
//            String answerTesseract = ocrTesseractService.read(bytes);
//            String userPrompt = """
//                    google cloud vision:
//                    %s
//
//                    tesseract:
//                    %s
//                    """.formatted(answerGCV, answerTesseract);
//
//            String response = chatClient.prompt()
//                    .user(userPrompt)
//                    .call()
//                    .content();
//
//            silent.send("GCV: \n" + answerGCV, chatId);
//            silent.send("Tesseract: \n" + answerTesseract, chatId);
//            silent.send(response, chatId);
//        } catch (Exception e) {
//            silent.send("Не удалось обработать изображение: " + e.getMessage(), chatId);
//        }
//    }
//
//    public Ability ai() {
//        return Ability
//                .builder()
//                .name("ai")
//                .info("Отправить текст в OpenAI")
//                .locality(org.telegram.abilitybots.api.objects.Locality.ALL)
//                .privacy(org.telegram.abilitybots.api.objects.Privacy.PUBLIC)
//                .action(this::handleAiCommand)
//                .build();
//    }
//
//    private void handleAiCommand(MessageContext ctx) {
//        String prompt = String.join(" ", ctx.arguments());
//        String response = chatClient.prompt()
//                .user(prompt)
//                .call()
//                .content();
//        silent.send(response, ctx.chatId());
//    }
//
//
//    public Reply handleCallbacks() {
//        return Reply.of((bot, update) -> {
//            CallbackQuery cq = update.getCallbackQuery();
//            String data = cq.getData();
//            long chatId = cq.getMessage().getChatId();
//
//            switch (data) {
//                case "ACTION_A" -> silent.send("Вы выбрали A ✅", chatId);
//                case "ACTION_B" -> silent.send("Вы выбрали B ✅", chatId);
//                default -> silent.send("Неизвестное действие 🤔", chatId);
//            }
//
//            if (cq.getMessage() instanceof Message) {
//                EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
//                        .chatId(cq.getMessage().getChatId().toString())
//                        .messageId(((Message) cq.getMessage()).getMessageId())
//                        .replyMarkup(null)
//                        .build();
//                silent.execute(edit);
//            }
//        }, Flag.CALLBACK_QUERY);
//    }
//
//    public Reply handleMessages() {
//        return Reply.of(this::reply,
//                Flag.TEXT,
//                update -> !update.getMessage().getText().startsWith("/"));
//    }
//
//    public Reply handlePhotos() {
//        return Reply.of(this::handleAiPhoto,
//                Flag.PHOTO);
//    }
//
//    private void reply(BaseAbilityBot bot, Update update) {
//        String text = update.getMessage().getText();
//        Long chatId = update.getMessage().getChatId();
//        Map<UUID, String> categories = categoryService.find().stream().
//                collect(Collectors.toMap(CategoriesRecord::getId, CategoriesRecord::getName));
//        String answer = chatClient.prompt()
//                .user(buildUserPrompt(text, categories))
//                .call()
//                .content();
//        silent.send(answer, chatId);
//        if (answer != null && !answer.isBlank()) {
//            try {
//                ReceiptDto dto = mapper.readValue(answer, ReceiptDto.class);
//                purchasesService.create(dto);
//                silent.send("created", chatId);
//            } catch (Exception e) {
//                logger.error("Error", e);
//                throw new RuntimeException(e);
//            }
//        } else {
//            silent.send("Ничего не понятно =(", chatId);
//        }
//    }
//
//    public static String buildUserPrompt(String message, Map<UUID, String> categories) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("<user_message>\n")
//                .append(message)
//                .append("\n</user_message>\n\n");
//
//        sb.append("Available categories:\n");
//        for (Map.Entry<UUID, String> categoryEntry : categories.entrySet()) {
//            sb.append("- ").append(categoryEntry.getKey()).append(":").append(categoryEntry.getValue()).append("\n");
//        }
//
//        return sb.toString();
//    }
//
//    private void replyPhoto(BaseAbilityBot bot, Update update) {
//        Long chatId = update.getMessage().getChatId();
//        silent.send("фото", chatId);
//    }
//
//    private String getCompanyName(String text, String command) {
//        return text.trim().substring(command.length()).trim();
//    }
//
//
//    @Override
//    public List<Reply> replies() {
//        return List.of(handlePhotos(), handleMessages(), handleCallbacks());
//    }
//
//    private ReplyKeyboardMarkup replyKeyboard() {
//        KeyboardRow row1 = new KeyboardRow(List.of(
//                KeyboardButton.builder().text("Помощь").build(),
//                KeyboardButton.builder().text("О боте").build()
//        ));
//        KeyboardRow row2 = new KeyboardRow(List.of(
//                KeyboardButton.builder().text("Поделиться контактом").requestContact(true).build()
//        ));
//        return ReplyKeyboardMarkup.builder()
//                .keyboard(List.of(row1, row2))
//                .resizeKeyboard(true)
//                .oneTimeKeyboard(true)
//                .build();
//    }
//
//    private InlineKeyboardMarkup inlineKeyboard() {
//        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
//        rows.add(List.of(
//                InlineKeyboardButton.builder().text("Кнопка A").callbackData("ACTION_A").build(),
//                InlineKeyboardButton.builder().text("Кнопка B").callbackData("ACTION_B").build()
//        ));
//        return InlineKeyboardMarkup.builder().keyboard(rows).build();
//    }
//
//    @Override
//    public long creatorId() {
//        return 1L;
//    }
//}
//
