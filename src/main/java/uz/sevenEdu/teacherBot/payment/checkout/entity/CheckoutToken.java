package uz.sevenEdu.teacherBot.payment.checkout.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Mobil → web checkout havolasi tokeni.
 * Mobil ilova token yaratadi, foydalanuvchi oazisedu.uz/?t=<token>
 * sahifasida to'lovni yakunlaydi. TTL — 15 daqiqa.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("checkout_tokens")
public class CheckoutToken {
    @Id
    private Long id;
    private String token;           // URL-safe Base64 (32 bayt SecureRandom)
    private Long userId;
    private String productType;     // 'course' | 'book'
    private Long productId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;   // to'lov muvaffaqiyatida belgilanadi
}
