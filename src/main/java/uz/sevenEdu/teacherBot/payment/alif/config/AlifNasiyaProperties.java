package uz.sevenEdu.teacherBot.payment.alif.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alif-nasiya")
public class AlifNasiyaProperties {
    private String shopId;
    private String secretKey;
}
