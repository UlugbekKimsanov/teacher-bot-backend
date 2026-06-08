package uz.sevenEdu.teacherBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import uz.sevenEdu.teacherBot.payment.click.config.ClickProperties;

@SpringBootApplication
@EnableConfigurationProperties(ClickProperties.class)
public class TeacherBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeacherBotApplication.class, args);
    }
}
