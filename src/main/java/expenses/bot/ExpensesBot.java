package expenses.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import expenses.dto.ExpensesAndCategoriesRecord;
import expenses.dto.OpenAiExpensesResponseDto;
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
import org.telegram.abilitybots.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
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

        List<String> categories = categoryService.find().stream().map(CategoriesRecord::getName).toList();
        OpenAiRequestDto request = new OpenAiRequestDto(text, categories);

        String answer = chatClient.prompt()
                .user(writeOpenAiRequestDto(request))
                .call()
                .content();
        logger.info("Answer from open ai : {}", answer);

        OpenAiExpensesResponseDto dto = readOpenAiExpensesResponseDto(answer);
        if (dto.expenses().isEmpty()) {
            silent.send("К сожалению, я вас не понял =(", chatId);
        } else {
           ExpensesAndCategoriesRecord saved = expensesService.createExpensesAndCategories(dto);
            silent.send(format(saved), chatId);
        }
    }

    private OpenAiExpensesResponseDto readOpenAiExpensesResponseDto(String text) {
        try {
            return mapper.readValue(text, OpenAiExpensesResponseDto.class);
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
                        String.format("%s (%s): %s, %,.2f x %d %s", e.getName(), e.getDescription(), categoryById.getOrDefault(e.getCategoryId(), "<unknown>"), e.getPrice(), e.getAmount(), e.getCurrency()))
                .toList()
                .toString();
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

