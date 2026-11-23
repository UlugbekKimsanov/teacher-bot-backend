package uz.sevenEdu.teacherBot.payment.mock;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.checkout.entity.CheckoutToken;
import uz.sevenEdu.teacherBot.payment.checkout.repository.CheckoutTokenRepository;
import uz.sevenEdu.teacherBot.payment.common.PaymentFulfillmentService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Lokal sinov uchun soxta to'lov provayderi (faqat "mock" profili).
 * Haqiqiy provayder sahifasi o'rniga oddiy sahifa ko'rsatadi:
 *  - "To'lash" → xarid haqiqiy webhook kabi yakunlanadi va landing'ga qaytaradi;
 *  - "Bekor qilish" → hech narsa o'zgarmay landing'ga qaytaradi.
 * Kurs uchun haqiqiy oqim (PaymentFulfillmentService) ishlaydi: kursga yozish, daromad yozuvi,
 * sovg'a kitoblar. Kitob uchun faqat kutubxona yozuvi yaratiladi — sotuv qaydi va adminlarga
 * bildirishnoma (sayt/Telegram) YUBORILMAYDI, chunki bu sinov.
 */
@RestController
@RequestMapping("/api/dev/mock-pay")
@ConditionalOnProperty(name = "app.mock-payments.enabled", havingValue = "true")
@RequiredArgsConstructor
public class MockPaymentController {

    private final CheckoutTokenRepository checkoutTokenRepository;
    private final PaymentFulfillmentService paymentFulfillmentService;
    private final DatabaseClient databaseClient;

    @Value("${app.mock-payments.return-url:http://localhost:3100/}")
    private String returnUrl;

    /** Soxta provayder sahifasi. */
    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public Mono<ResponseEntity<String>> page(@RequestParam("token") String token) {
        return validToken(token)
                .map(t -> ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html(t)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<!doctype html><meta charset=\"utf-8\"><p>To'lov havolasi yaroqsiz yoki muddati o'tgan.</p>"
                                + "<p><a href=\"" + HtmlUtils.htmlEscape(returnUrl) + "\">Saytga qaytish</a></p>"));
    }

    /** "To'lash" bosilganda: xaridni yakunlaydi va landing'ga qaytaradi. */
    @GetMapping("/confirm")
    public Mono<ResponseEntity<Void>> confirm(@RequestParam("token") String token) {
        return validToken(token)
                .flatMap(t -> fulfil(t).thenReturn(true))
                .defaultIfEmpty(false)
                .map(ignored -> ResponseEntity.status(HttpStatus.FOUND).location(URI.create(returnUrl)).<Void>build());
    }

    private Mono<Void> fulfil(CheckoutToken t) {
        if ("course".equals(t.getProductType())) {
            return paymentFulfillmentService.onPaymentSuccess(
                    "course_" + t.getUserId() + "_" + t.getProductId(), MockPaymentSetup.METHOD_CODE);
        }
        // Kitob: faqat kutubxona yozuvi (sotuv qaydi va bildirishnomalarsiz) + tokenni ishlatilgan deb belgilash
        return databaseClient.sql("INSERT INTO user_books (user_id, book_id, payment_method, is_active, purchased_at) "
                        + "VALUES (:userId, :bookId, '" + MockPaymentSetup.METHOD_CODE + "', TRUE, NOW()) "
                        + "ON CONFLICT (user_id, book_id) DO UPDATE SET is_active = TRUE")
                .bind("userId", t.getUserId())
                .bind("bookId", t.getProductId())
                .fetch().rowsUpdated()
                .then(checkoutTokenRepository.markUsedByUserAndProduct(t.getUserId(), "book", t.getProductId()))
                .then();
    }

    private Mono<CheckoutToken> validToken(String token) {
        if (token == null || token.isBlank()) return Mono.empty();
        return checkoutTokenRepository.findByToken(token)
                .filter(t -> t.getUsedAt() == null
                        && t.getExpiresAt() != null
                        && t.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    private String html(CheckoutToken t) {
        String confirmUrl = "/api/dev/mock-pay/confirm?token=" + URLEncoder.encode(t.getToken(), StandardCharsets.UTF_8);
        String product = ("course".equals(t.getProductType()) ? "Kurs" : "Kitob") + " #" + t.getProductId();
        return """
                <!doctype html>
                <html lang="uz">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Mock to'lov (lokal sinov)</title>
                  <style>
                    body { margin:0; min-height:100vh; display:flex; align-items:center; justify-content:center;
                           background:#E8F5EE; font-family:system-ui,-apple-system,'Segoe UI',sans-serif; color:#1a1a1a; }
                    .card { background:#fff; border-radius:24px; padding:32px; width:min(92vw,420px);
                            box-shadow:0 12px 40px rgba(31,122,85,.18); text-align:center; }
                    .tag { display:inline-block; background:#FEF3C7; color:#92400E; border-radius:999px;
                           padding:4px 12px; font-size:12px; font-weight:700; }
                    h1 { font-size:22px; margin:16px 0 8px; }
                    p { color:#555; font-size:14px; line-height:1.6; margin:0 0 24px; }
                    a { display:block; border-radius:12px; padding:14px; font-weight:700; font-size:15px;
                        text-decoration:none; margin-top:10px; }
                    .pay { background:#2E9E6E; color:#fff; }
                    .cancel { border:2px solid #e5e7eb; color:#555; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <span class="tag">LOKAL SINOV — haqiqiy to'lov emas</span>
                    <h1>Mock to'lov</h1>
                    <p>%s uchun to'lov. Tugmalardan birini bosing — natijaga qarab saytga qaytasiz.</p>
                    <a class="pay" href="%s">To'lash (muvaffaqiyatli)</a>
                    <a class="cancel" href="%s">Bekor qilish</a>
                  </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(product), HtmlUtils.htmlEscape(confirmUrl),
                HtmlUtils.htmlEscape(returnUrl));
    }
}
