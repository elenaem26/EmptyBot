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
//--TODO add userId, id ввезде пусть будет bigserial? или uuid generated
//--TODO изменять позапрошлую и тд покупку
//--менеджерить категории
//--удалить экспенс?
//        --default currency to show
//команды - покажи все покупки, покажи категории и тд... - и также менеджерить покупки