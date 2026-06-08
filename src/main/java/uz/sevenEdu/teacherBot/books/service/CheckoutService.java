package uz.sevenEdu.teacherBot.books.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;
import uz.sevenEdu.teacherBot.payment.click.config.ClickProperties;
import uz.sevenEdu.teacherBot.payment.payme.config.PaymeProperties;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Har bir to'lov tizimi uchun checkout URL yaratadi.
 * merchantTransId formati: "book_{userId}_{bookId}"
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final BooksRepository booksRepository;
    private final ClickProperties clickProperties;
    private final PaymeProperties paymeProperties;

    public Mono<Map<String, String>> createCheckoutUrl(Long userId, Long bookId, String paymentMethod) {
        String merchantTransId = "book_" + userId + "_" + bookId;

        return booksRepository.findById(bookId)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .map(book -> {
                    int priceSum = book.getPrice() != null ? book.getPrice() : 0;
                    long priceTiyin = priceSum * 100L;

                    String checkoutUrl = switch (paymentMethod.toLowerCase()) {
                        case "click" -> buildClickUrl(merchantTransId, priceSum);
                        case "payme" -> buildPaymeUrl(merchantTransId, priceTiyin);
                        case "paynet" -> buildPaynetUrl(merchantTransId, priceSum);
                        case "uzum nasiya", "uzumnasiya" -> buildUzumNasiyaUrl(merchantTransId, priceSum);
                        case "alif nasiya", "alifnasiya" -> buildAlifNasiyaUrl(merchantTransId, priceSum);
                        default -> throw new RuntimeException("Noto'g'ri to'lov usuli: " + paymentMethod);
                    };

                    Map<String, String> result = new HashMap<>();
                    result.put("checkoutUrl", checkoutUrl);
                    result.put("merchantTransId", merchantTransId);
                    return result;
                });
    }

    /**
     * Click checkout URL
     * https://my.click.uz/services/pay?service_id=X&merchant_id=X&amount=X&transaction_param=X
     */
    private String buildClickUrl(String merchantTransId, int amountSum) {
        return "https://my.click.uz/services/pay"
                + "?service_id=" + clickProperties.getServiceId()
                + "&merchant_id=" + clickProperties.getMerchantId()
                + "&amount=" + amountSum
                + "&transaction_param=" + encode(merchantTransId)
                + "&return_url=" + encode("https://7edu.uz/payment/success");
    }

    /**
     * Payme checkout URL
     * https://checkout.paycom.uz/{base64_params}
     * params: m={merchantId};ac.order_id={merchantTransId};a={amount_tiyin}
     */
    private String buildPaymeUrl(String merchantTransId, long amountTiyin) {
        String params = "m=" + paymeProperties.getMerchantId()
                + ";ac.order_id=" + merchantTransId
                + ";a=" + amountTiyin;
        String encoded = Base64.getEncoder().encodeToString(params.getBytes(StandardCharsets.UTF_8));
        return "https://checkout.paycom.uz/" + encoded;
    }

    /**
     * Paynet checkout URL
     * https://paynet.uz/checkout?merchant_trans_id=X&amount=X
     */
    private String buildPaynetUrl(String merchantTransId, int amountSum) {
        return "https://paynet.uz/checkout"
                + "?merchant_trans_id=" + encode(merchantTransId)
                + "&amount=" + amountSum;
    }

    /**
     * Uzum Nasiya checkout URL
     * https://nasiya.uzum.uz/checkout?order_id=X&amount=X
     */
    private String buildUzumNasiyaUrl(String merchantTransId, int amountSum) {
        return "https://nasiya.uzum.uz/checkout"
                + "?order_id=" + encode(merchantTransId)
                + "&amount=" + amountSum;
    }

    /**
     * Alif Nasiya checkout URL (bePaid gateway)
     * https://alifnasiya.uz/checkout?order_id=X&amount=X
     */
    private String buildAlifNasiyaUrl(String merchantTransId, int amountSum) {
        return "https://alifnasiya.uz/checkout"
                + "?order_id=" + encode(merchantTransId)
                + "&amount=" + amountSum;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
