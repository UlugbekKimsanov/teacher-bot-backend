package uz.sevenEdu.teacherBot.payment.click.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickResponseDto {

    @JsonProperty("click_trans_id")
    private Long clickTransId;

    @JsonProperty("merchant_trans_id")
    private String merchantTransId;

    @JsonProperty("merchant_prepare_id")
    private Long merchantPrepareId;

    @JsonProperty("merchant_confirm_id")
    private Long merchantConfirmId;

    @JsonProperty("error")
    private Integer error;

    @JsonProperty("error_note")
    private String errorNote;
}
