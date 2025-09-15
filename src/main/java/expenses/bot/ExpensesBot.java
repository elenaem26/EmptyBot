package expenses.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import expenses.dto.ExpensesAndCategoriesRecord;
import expenses.dto.OpenAiExpenseDto;
import expenses.dto.OpenAiRequestDto;
import expenses.jooq.generated.tables.records.CategoriesRecord;
import expenses.jooq.generated.tables.records.ExpensesRecord;
import expenses.service.CategoryService;
import expenses.service.ExpensesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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

@Service
public class ExpensesBot extends AbilityBot {

    private final Logger logger = LoggerFactory.getLogger(ExpensesBot.class);

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ExpensesService expensesService;

    @Autowired
    private ChatClient chatClient;

    private final ObjectMapper mapper = new ObjectMapper();

    public ExpensesBot(@Value("${telegrambot.name}") String botName, @Value("${telegrambot.token}") String botToken) {
        super(botToken, botName);
    }

    public Reply handleMessages() {
        return Reply.of(this::reply,
                Flag.TEXT,
                update -> !update.getMessage().getText().startsWith("/"));
    }

    private void reply(BaseAbilityBot bot, Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        if (text.isBlank()) {
            silent.send("Строка пустая =(", chatId);;
        }

        Map<String, CategoriesRecord> categoriesByName = categoryService.find()
                .stream()
                .collect(Collectors.toMap(CategoriesRecord::getName, c -> c));
        OpenAiRequestDto request = new OpenAiRequestDto(text, categoriesByName.keySet());

        String answer = chatClient.prompt()
                .user(writeOpenAiRequestDto(request))
                .call()
                .content();
        logger.info("Answer from open ai : {}", answer);

        OpenAiExpenseDto dto = readOpenAiExpensesResponseDto(answer);
        if (dto == null) {
            silent.send("К сожалению, я вас не понял =(", chatId);
        } else {
            CategoriesRecord category;
            boolean isNew = false;
            if (dto.category() == null) {
                category = categoryService.createCategory(dto.name());
                isNew = true;
            } else {
                category = categoriesByName.get(dto.category());
            }

            ExpensesRecord saved = expensesService.createExpensesAndCategories(dto, category);
            silent.send(format(saved, category, isNew), chatId);
        }
    }

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

    private String format(ExpensesAndCategoriesRecord saved) {
        List<ExpensesRecord> expenses = saved.expenses();
        Map<UUID, String> categoryById = saved.categories().stream().collect(Collectors.toMap(CategoriesRecord::getId, CategoriesRecord::getName));
        return expenses.stream()
                .map(e ->
                        String.format("%s: %s, %,.2f %s", e.getName(), categoryById.getOrDefault(e.getCategoryId(), "<unknown>"), e.getPrice(), e.getCurrency()))
                .toList()
                .toString();
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

