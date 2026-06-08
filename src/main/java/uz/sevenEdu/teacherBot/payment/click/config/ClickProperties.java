package uz.sevenEdu.teacherBot.payment.click.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "click")
public class ClickProperties {
    private Long merchantId = 24180L;
    private Long serviceId = 31879L;
    private Long merchantUserId = 0L;
    private String secretKey = "LuGG43iQVdGSWZDiY";
}
