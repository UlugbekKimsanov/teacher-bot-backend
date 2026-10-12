package uz.sevenEdu.teacherBot.payment.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Landing checkout sahifasida ko'rsatiladigan to'lov usuli. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutMethodDto {
    private String code;        // click, payme, paynet, uzumnasiya, alifnasiya
    private String name;
    private Boolean isNasiya;
}
