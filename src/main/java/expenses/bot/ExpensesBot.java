package expenses.bot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import expenses.configuration.BotConfiguration;
import expenses.dto.ExpensesAndCategoriesRecord;
import expenses.dto.OpenAiExpenseDto;
import expenses.dto.OpenAiRequestDto;
import expenses.dto.v2.ExpenseAction;
import expenses.jooq.generated.tables.records.CategoriesRecord;
import expenses.jooq.generated.tables.records.ExpensesRecord;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static expenses.bot.ExpenseFormatter.formatResponse;

@Service
public class ExpensesBot extends AbilityBot {

    private final Logger logger = LoggerFactory.getLogger(ExpensesBot.class);

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ExpensesService expensesService;

    @Autowired
    private BotConfiguration botConfiguration;

    @Autowired
    private ChatClient chatClient;

    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private ObjectMapper objectMapper;

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
            silent.send("Строка пустая =(", chatId);;
        }

        ExpenseAction action = parseMessage(text);
        if (action.action() == ExpenseAction.Action.CREATE_TRANSACTION) {
            expensesService.insertExpense(userId, action.transaction());
        }

        silent.send(formatResponse(action), chatId);
    }


    private ExpenseAction parseMessage(String userText) {

        var options = OpenAiChatOptions.builder()
                .responseFormat(
                        new ResponseFormat(
                                ResponseFormat.Type.JSON_SCHEMA,
                                botConfiguration.expenseJsonSchema()
                        )
                )
                .build();

        return chatClient.prompt()
                .user(userSpec -> userSpec.text(userText))
                .options(options)
                .call()
                .entity(ExpenseAction.class);
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

    private OpenAiExpenseDto readOpenAiExpensesResponseDto(String text) {
        try {
            return mapper.readValue(text, OpenAiExpenseDto.class);
        } catch (Exception e) {
            logger.error("Exception while parsing: ", e);
            throw new RuntimeException(e);
        }
    }

    private String writeOpenAiRequestDto(OpenAiRequestDto requestDto) {
        try {
            return mapper.writeValueAsString(requestDto);
        } catch (Exception e) {
            logger.error("Exception while parsing: ", e);
            throw new RuntimeException(e);
        }
    }

    private String format(ExpensesRecord saved, CategoriesRecord category, boolean isNew) {
        StringBuilder sb = new StringBuilder();
        if (isNew) {
            sb.append("Новая категория: ");
        } else {
            sb.append("Старая категория: ");
        }
        sb.append(category.getName()).append("\n");
        sb.append(String.format("%s %,.2f %s", saved.getName(), saved.getPrice(), saved.getCurrency()));
        return sb.toString();
    }

    @Override
    public List<Reply> replies() {
        return List.of(handleMessages());
    }

    @Override
    public long creatorId() {
        return 1L;
    }
}

