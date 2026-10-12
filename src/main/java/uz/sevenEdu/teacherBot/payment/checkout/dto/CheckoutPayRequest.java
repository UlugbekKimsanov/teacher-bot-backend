package uz.sevenEdu.teacherBot.payment.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** POST /api/landing/checkout/{token}/pay so'rov tanasi. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutPayRequest {
    private String method; // click | payme | paynet | uzumnasiya | alifnasiya
}
