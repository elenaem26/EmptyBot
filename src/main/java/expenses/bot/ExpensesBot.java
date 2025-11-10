package expenses.bot;

import expenses.configuration.BotConfiguration;
import expenses.dto.v2.ExpenseAction;
import expenses.service.CategoryRuleService;
import expenses.service.CategoryService;
import expenses.service.ExpensesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static expenses.bot.ExpenseFormatter.formatResponse;

@Service
public class ExpensesBot extends AbilityBot {

    private final Logger logger = LoggerFactory.getLogger(ExpensesBot.class);

    @Autowired
    private CategoryRuleService categoryRuleService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ExpensesService expensesService;

    @Autowired
    private BotConfiguration botConfiguration;

    @Autowired
    private ChatClient chatClient;

    public ExpensesBot(@Value("${telegrambot.name}") String botName, @Value("${telegrambot.token}") String botToken) {
        super(botToken, botName);
    }

    public Reply handleMessages() {
        return Reply.of(this::replyv2,
                Flag.TEXT,
                update -> !update.getMessage().getText().startsWith("/"));
    }

    private void replyv2(BaseAbilityBot bot, Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();
        if (text.isBlank()) {
            silent.send("Строка пустая =(", chatId);
        }

        String categories = categoryService.listCategoriesAsString(userId); //todo cache?
        String rules = categoryRuleService.listRulesAsString(userId);

        ExpenseAction action = sendToOpenAi(text, categories, rules);
        switch (action.action()) {
            case CREATE_TRANSACTION -> expensesService.insertExpense(userId, action.transaction());
            case UPDATE_TRANSACTION -> expensesService.updateExpense(userId, action.update());
        }

        silent.send(formatResponse(action), chatId);
    }


    private ExpenseAction sendToOpenAi(String userText, String categories, String rules) {
        var options = OpenAiChatOptions.builder()
                .responseFormat(
                        new ResponseFormat(
                                ResponseFormat.Type.JSON_SCHEMA,
                                botConfiguration.expenseJsonSchema()
                        )
                )
                .build();

        return chatClient.prompt()
                .user(userSpec -> userSpec.text(buildUserPrompt(userText, categories, rules)))
                .options(options)
                .call()
                .entity(ExpenseAction.class);
    }

    public String buildUserPrompt(String userText, String categories, String rules) {
        return "Сообщение пользователя: " +
                userText +
                "Категории пользователя: " +
                categories +
                "Правила пользователя: " +
                rules;
    }

//    private void reply(BaseAbilityBot bot, Update update) {
//        String text = update.getMessage().getText();
//        Long chatId = update.getMessage().getChatId();
//        if (text.isBlank()) {
//            silent.send("Строка пустая =(", chatId);;
//        }
//
//        Map<String, CategoriesRecord> categoriesByName = categoryService.find()
//                .stream()
//                .collect(Collectors.toMap(CategoriesRecord::getName, c -> c));
//        OpenAiRequestDto request = new OpenAiRequestDto(text, categoriesByName.keySet());
//
//        String answer = chatClient.prompt()
//                .user(writeOpenAiRequestDto(request))
//                .call()
//                .content();
//        logger.info("Answer from open ai : {}", answer);
//
//        OpenAiExpenseDto dto = readOpenAiExpensesResponseDto(answer);
//        if (dto == null) {
//            silent.send("К сожалению, я вас не понял =(", chatId);
//        } else {
//            CategoriesRecord category;
//            boolean isNew = false;
//            if (dto.category() == null) {
//                category = categoryService.createCategory(dto.suggestCategory());
//                isNew = true;
//            } else {
//                category = categoriesByName.get(dto.category());
//            }
//
//            ExpensesRecord saved = expensesService.createExpensesAndCategories(dto, category);
//            silent.send(format(saved, category, isNew), chatId);
//        }
//    }


    @Override
    public List<Reply> replies() {
        return List.of(handleMessages());
    }

    @Override
    public long creatorId() {
        return 1L;
    }
}

