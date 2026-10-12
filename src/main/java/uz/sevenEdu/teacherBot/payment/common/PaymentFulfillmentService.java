package uz.sevenEdu.teacherBot.payment.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.service.BooksService;
import uz.sevenEdu.teacherBot.course.entity.UserCourse;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.payment.checkout.repository.CheckoutTokenRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * To'lov MUVAFFAQIYATLI yakunlanganda bajariladigan umumiy oqim —
 * barcha provider webhook'lari (Payme, Paynet, Click, Uzum, Alif) shu
 * servisni chaqiradi:
 *  - "book_{userId}_{bookId}"     → kitobni faollashtirish (BooksService.purchaseBook)
 *  - "course_{userId}_{courseId}" → kursga avtomatik yozish (UserCourse to'g'ridan-to'g'ri,
 *    CourseServiceImpl.enrollCourse chaqirilmaydi — u premium gate bilan 403 qaytaradi)
 * Ikkala holatda ham tegishli checkout_tokens.used_at belgilanadi
 * (web checkout havolasi qayta ishlatilmasligi uchun).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFulfillmentService {

    private final BooksService booksService;
    private final UserCourseRepository userCourseRepository;
    private final CheckoutTokenRepository checkoutTokenRepository;

    /**
     * merchantTransId prefiksiga qarab mos oqimni bajaradi.
     * Format noto'g'ri bo'lsa hech narsa qilmaydi (webhook javobini buzmaslik uchun).
     */
    public Mono<Void> onPaymentSuccess(String merchantTransId, String paymentMethod) {
        if (merchantTransId == null) return Mono.empty();
        String[] parts = merchantTransId.split("_");
        if (parts.length != 3) return Mono.empty();

        Long userId;
        Long productId;
        try {
            userId = Long.parseLong(parts[1]);
            productId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return Mono.empty();
        }

        if ("book".equals(parts[0])) {
            Long bookId = productId;
            return booksService.purchaseBook(userId, bookId, paymentMethod, merchantTransId)
                    .then(consumeTokens(userId, "book", bookId));
        }
        if ("course".equals(parts[0])) {
            Long courseId = productId;
            return enrollPaidCourse(userId, courseId)
                    .then(consumeTokens(userId, "course", courseId));
        }
        return Mono.empty();
    }

    /**
     * To'langan kursga yozish — duplikat oldini oladi (mavjud bo'lsa qayta yaratmaydi).
     */
    private Mono<Void> enrollPaidCourse(Long userId, Long courseId) {
        return userCourseRepository.existsByUserIdAndCourseId(userId, courseId)
                .flatMap(exists -> exists
                        ? Mono.<UserCourse>empty()
                        : userCourseRepository.save(UserCourse.builder()
                                .userId(userId)
                                .courseId(courseId)
                                .progress(BigDecimal.ZERO)
                                .createdAt(LocalDateTime.now())
                                .build()))
                .then();
    }

    /**
     * User+mahsulot bo'yicha faol checkout tokenlarni ishlatilgan deb belgilaydi.
     * Xato bo'lsa to'lov oqimini buzmaydi (token belgilash — ikkilamchi amal).
     */
    private Mono<Void> consumeTokens(Long userId, String productType, Long productId) {
        return checkoutTokenRepository.markUsedByUserAndProduct(userId, productType, productId)
                .doOnError(e -> log.warn("checkout_tokens.used_at belgilashda xato: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}
