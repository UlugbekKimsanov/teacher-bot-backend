package uz.sevenEdu.teacherBot.payment.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** POST /api/v1/checkout-links javobi — web checkout havolasi. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutLinkDto {
    private String url;             // https://oazisedu.uz/?t=<token>
    private String token;
    private LocalDateTime expiresAt;
}
