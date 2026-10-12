package uz.sevenEdu.teacherBot.payment.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.entity.Books;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;

/**
 * To'lov webhook'larida kelgan summani kitobning haqiqiy (kutilgan) narxi
 * bilan solishtirish uchun YAGONA manba. Barcha provider servislari
 * (Payme, Paynet, Click, Uzum Nasiya, Alif Nasiya) shu servisdan foydalanadi.
 *
 * DIQQAT: narx formulasi CheckoutService.createCheckoutUrl dagi hisob bilan
 * AYNAN bir xil bo'lishi shart (kitob narxi + bosma kitob uchun PAID
 * yetkazib berish narxi), aks holda haqiqiy to'lovlar rad etiladi.
 *
 * Birliklar:
 *  - expectedAmountSum   — SO'M  (Click, Uzum Nasiya, Alif Nasiya)
 *  - expectedAmountTiyin — TIYIN (Payme, Paynet; 1 so'm = 100 tiyin)
 */
@Service
@RequiredArgsConstructor
public class BookPaymentAmountService {

    private final BooksRepository booksRepository;

    /**
     * merchantTransId ("book_{userId}_{bookId}") dan bookId ni ajratadi.
     * Format noto'g'ri bo'lsa null qaytaradi.
     */
    public Long parseBookId(String merchantTransId) {
        if (merchantTransId == null) return null;
        String[] parts = merchantTransId.split("_");
        if (parts.length != 3 || !"book".equals(parts[0])) return null;
        try {
            Long.parseLong(parts[1]); // userId ham raqam bo'lishi shart
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Kutilgan to'lov summasi SO'MDA.
     * Kitob topilmasa yoki merchantTransId formati noto'g'ri bo'lsa — bo'sh Mono.
     */
    public Mono<Long> expectedAmountSum(String merchantTransId) {
        Long bookId = parseBookId(merchantTransId);
        if (bookId == null) return Mono.empty();
        return booksRepository.findById(bookId).map(this::priceSum);
    }

    /**
     * Kutilgan to'lov summasi TIYINDA (so'm × 100).
     */
    public Mono<Long> expectedAmountTiyin(String merchantTransId) {
        return expectedAmountSum(merchantTransId).map(sum -> sum * 100L);
    }

    /** CheckoutService.createCheckoutUrl dagi formula bilan AYNAN bir xil. */
    private long priceSum(Books book) {
        long priceSum = book.getPrice() != null ? book.getPrice() : 0;
        // Bosma kitob + pullik yetkazib berish — umumiy to'lov summasiga qo'shamiz
        if ("print".equalsIgnoreCase(book.getCategory())
                && "PAID".equalsIgnoreCase(book.getDeliveryType())
                && book.getDeliveryPrice() != null) {
            priceSum += book.getDeliveryPrice();
        }
        return priceSum;
    }
}
