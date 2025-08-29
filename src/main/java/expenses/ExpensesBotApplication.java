package expenses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class ExpensesBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpensesBotApplication.class, args);
    }

}
