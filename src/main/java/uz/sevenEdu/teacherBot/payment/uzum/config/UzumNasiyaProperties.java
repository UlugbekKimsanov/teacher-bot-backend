package uz.sevenEdu.teacherBot.payment.uzum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "uzum-nasiya")
public class UzumNasiyaProperties {
    private String username;
    private String password;
    private int serviceId;
}
