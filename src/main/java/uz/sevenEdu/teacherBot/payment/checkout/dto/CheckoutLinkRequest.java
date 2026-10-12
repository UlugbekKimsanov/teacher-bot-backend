package uz.sevenEdu.teacherBot.payment.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** POST /api/v1/checkout-links so'rov tanasi. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutLinkRequest {
    private String productType; // 'course' | 'book'
    private Long productId;
}
