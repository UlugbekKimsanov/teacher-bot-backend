package uz.sevenEdu.teacherBot.payment.paynet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "paynet")
public class PaynetProperties {
    private String username;
    private String password;
    private int serviceId;
}
