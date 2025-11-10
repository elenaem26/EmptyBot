package expenses.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SchemaProvider {

    private final String EXPENSE_SCHEMA;

    public SchemaProvider() {
        try (var in = new ClassPathResource("schema/expense-schema.json").getInputStream()) {
            this.EXPENSE_SCHEMA = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read schema file: schema/expense-schema.json", e);
        }
    }

    public String getExpenseSchema() {
        return EXPENSE_SCHEMA;
    }
}

