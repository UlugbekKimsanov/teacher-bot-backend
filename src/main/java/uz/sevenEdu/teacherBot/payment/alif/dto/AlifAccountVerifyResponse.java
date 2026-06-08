package uz.sevenEdu.teacherBot.payment.alif.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlifAccountVerifyResponse {
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("tracking_id")
    private String trackingId;

    private Long amount;
    private String currency;

    /** 0 = OK, 5 = account not found, 241 = amount exceeds limit */
    private int result;

    private String description;

    public static AlifAccountVerifyResponse ok(String transactionId, Long amount, String currency) {
        return AlifAccountVerifyResponse.builder()
                .transactionId(transactionId)
                .trackingId(transactionId)
                .amount(amount)
                .currency(currency)
                .result(0)
                .description("OK")
                .build();
    }

    public static AlifAccountVerifyResponse error(int code, String desc) {
        return AlifAccountVerifyResponse.builder()
                .result(code)
                .description(desc)
                .build();
    }
}
