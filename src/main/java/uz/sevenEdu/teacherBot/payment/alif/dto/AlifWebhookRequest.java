package uz.sevenEdu.teacherBot.payment.alif.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AlifWebhookRequest {
    private String uid;       // bePaid transaction UID
    private String status;    // "successful", "failed", "expired", "pending"
    private Long amount;
    private String currency;

    @JsonProperty("tracking_id")
    private String trackingId;  // merchantTransId

    @JsonProperty("shop_id")
    private String shopId;

    @JsonProperty("secret_key")
    private String secretKey;
}
