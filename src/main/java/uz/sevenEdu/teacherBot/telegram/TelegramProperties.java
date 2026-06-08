package uz.sevenEdu.teacherBot.telegram;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private String botToken;
    private String botUsername;
}
