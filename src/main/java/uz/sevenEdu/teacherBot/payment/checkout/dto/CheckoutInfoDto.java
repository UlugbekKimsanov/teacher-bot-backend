package uz.sevenEdu.teacherBot.payment.checkout.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** GET /api/landing/checkout/{token} javobi — web checkout sahifa ma'lumoti. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutInfoDto {
    private String productType;             // 'course' | 'book'
    private Long productId;
    private String title;                   // kurs nomi yoki kitob sarlavhasi
    private String priceLabel;              // formatlangan, masalan "199 000 so'm"
    private Long amountSum;                 // jami so'm (kitobda PAID yetkazib berish qo'shilgan)
    private String deliveryLabel;           // faqat bosma (print) kitob uchun
    private String userName;                // firstName + lastName
    private List<CheckoutMethodDto> methods;
    private LocalDateTime expiresAt;
}
