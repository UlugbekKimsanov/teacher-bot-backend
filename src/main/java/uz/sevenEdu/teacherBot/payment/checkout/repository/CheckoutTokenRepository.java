package uz.sevenEdu.teacherBot.payment.checkout.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.checkout.entity.CheckoutToken;

@Repository
public interface CheckoutTokenRepository extends ReactiveCrudRepository<CheckoutToken, Long> {

    Mono<CheckoutToken> findByToken(String token);

    /**
     * To'lov muvaffaqiyatida (webhook) user+mahsulot bo'yicha barcha faol
     * tokenlarni ishlatilgan deb belgilaydi — to'langan token qayta ishlatilmaydi,
     * to'lov muvaffaqiyatsiz bo'lsa esa token tirik qoladi (retry mumkin).
     */
    @Modifying
    @Query("""
            UPDATE checkout_tokens
            SET used_at = now()
            WHERE user_id = :userId
              AND product_type = :productType
              AND product_id = :productId
              AND used_at IS NULL
            """)
    Mono<Integer> markUsedByUserAndProduct(Long userId, String productType, Long productId);
}
