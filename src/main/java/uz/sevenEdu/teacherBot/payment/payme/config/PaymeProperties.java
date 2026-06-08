package uz.sevenEdu.teacherBot.payment.payme.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payme")
public class PaymeProperties {
    private String merchantId;
    private String secretKey;
}
