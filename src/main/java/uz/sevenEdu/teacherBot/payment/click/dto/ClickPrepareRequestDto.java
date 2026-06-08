package uz.sevenEdu.teacherBot.payment.click.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Click tomonidan prepare (action=0) so'rovida yuboriladigan ma'lumotlar.
 */
@Data
public class ClickPrepareRequestDto {

    @JsonProperty("click_trans_id")
    private Long clickTransId;

    @JsonProperty("service_id")
    private Long serviceId;

    @JsonProperty("click_paydoc_id")
    private Long clickPaydocId;

    @JsonProperty("merchant_trans_id")
    private String merchantTransId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("action")
    private Integer action;

    @JsonProperty("error")
    private Integer error;

    @JsonProperty("error_note")
    private String errorNote;

    @JsonProperty("sign_time")
    private String signTime;

    @JsonProperty("sign_string")
    private String signString;
}
