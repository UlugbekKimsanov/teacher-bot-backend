package uz.sevenEdu.teacherBot.payment.alif.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.alif.config.AlifNasiyaProperties;
import uz.sevenEdu.teacherBot.payment.common.BookPaymentAmountService;
import uz.sevenEdu.teacherBot.payment.common.CoursePaymentAmountService;
import uz.sevenEdu.teacherBot.payment.common.PaymentFulfillmentService;
import uz.sevenEdu.teacherBot.payment.alif.dto.AlifAccountVerifyRequest;
import uz.sevenEdu.teacherBot.payment.alif.dto.AlifAccountVerifyResponse;
import uz.sevenEdu.teacherBot.payment.alif.dto.AlifWebhookRequest;
import uz.sevenEdu.teacherBot.payment.alif.entity.AlifTransaction;
import uz.sevenEdu.teacherBot.payment.alif.enums.AlifTransactionStatus;
import uz.sevenEdu.teacherBot.payment.alif.repository.AlifTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AlifNasiyaService {

    private final AlifTransactionRepository repository;
    private final AlifNasiyaProperties properties;
    private final BookPaymentAmountService bookPaymentAmountService;
    private final CoursePaymentAmountService coursePaymentAmountService;
    private final PaymentFulfillmentService paymentFulfillmentService;

    /**
     * bePaid chaqiradi — mijoz akkauntini tekshirish.
     * merchantTransId = account = "book_{userId}_{bookId}" yoki "course_{userId}_{courseId}"
     */
    public Mono<AlifAccountVerifyResponse> verifyAccount(AlifAccountVerifyRequest req, String authHeader) {
        if (!isAuthorized(authHeader)) {
            return Mono.just(AlifAccountVerifyResponse.error(5, "Ruxsat yo'q"));
        }

        String merchantTransId = req.getAccount();
        if (merchantTransId == null
                || !(merchantTransId.startsWith("book_") || merchantTransId.startsWith("course_"))) {
            return Mono.just(AlifAccountVerifyResponse.error(5, "Account topilmadi"));
        }
        if (req.getAmount() == null || req.getAmount() <= 0) {
            return Mono.just(AlifAccountVerifyResponse.error(241, "Miqdor noto'g'ri"));
        }

        // Kelgan summa (so'm) mahsulotning (kitob/kurs) kutilgan narxi bilan qat'iy solishtiriladi
        return expectedAmountSum(merchantTransId)
                .flatMap(expected -> {
                    if (!expected.equals(req.getAmount())) {
                        return Mono.just(AlifAccountVerifyResponse.error(241,
                                "Miqdor mahsulot narxiga mos emas"));
                    }
                    // Tranzaksiya yaratish (PENDING)
                    return repository.findByMerchantTransId(merchantTransId)
                            .flatMap(existing -> Mono.just(AlifAccountVerifyResponse.ok(
                                    existing.getId().toString(), req.getAmount(), req.getCurrency())))
                            .switchIfEmpty(Mono.defer(() -> {
                                AlifTransaction tx = AlifTransaction.builder()
                                        .bepaidUid(req.getId())
                                        .merchantTransId(merchantTransId)
                                        .amount(BigDecimal.valueOf(req.getAmount()))
                                        .currency(req.getCurrency() != null ? req.getCurrency() : "UZS")
                                        .status(AlifTransactionStatus.PENDING)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();
                                return repository.save(tx)
                                        .map(saved -> AlifAccountVerifyResponse.ok(
                                                saved.getId().toString(), req.getAmount(), saved.getCurrency()));
                            }));
                })
                .switchIfEmpty(Mono.just(AlifAccountVerifyResponse.error(5, "Mahsulot topilmadi")));
    }

    /**
     * bePaid webhook — tranzaksiya natijasi.
     * shopId va secretKey ni tekshiradi.
     */
    public Mono<Void> handleWebhook(AlifWebhookRequest req) {
        // Credentials tekshirish
        if (!properties.getShopId().equals(req.getShopId()) ||
            !properties.getSecretKey().equals(req.getSecretKey())) {
            return Mono.empty();
        }

        String uid = req.getUid();
        String trackingId = req.getTrackingId(); // merchantTransId

        return repository.findByBepaidUid(uid)
                .switchIfEmpty(
                    // UID bo'yicha topilmasa, trackingId bo'yicha qidirish
                    trackingId != null
                        ? repository.findByMerchantTransId(trackingId)
                        : Mono.empty()
                )
                .flatMap(tx -> {
                    AlifTransactionStatus newStatus = mapStatus(req.getStatus());
                    if (tx.getStatus() == AlifTransactionStatus.SUCCESSFUL) {
                        return Mono.empty(); // allaqachon to'langan
                    }
                    tx.setStatus(newStatus);
                    tx.setBepaidUid(uid);
                    tx.setUpdatedAt(LocalDateTime.now());
                    return repository.save(tx)
                            .flatMap(saved -> {
                                if (saved.getStatus() == AlifTransactionStatus.SUCCESSFUL) {
                                    // Faollashtirishdan oldin summa (so'm) kutilgan mahsulot
                                    // (kitob/kurs) narxi bilan qat'iy solishtiriladi: webhook'dagi
                                    // amount, u bo'lmasa tranzaksiyada saqlangan summa
                                    BigDecimal paid = req.getAmount() != null
                                            ? BigDecimal.valueOf(req.getAmount())
                                            : saved.getAmount();
                                    return expectedAmountSum(saved.getMerchantTransId())
                                            .flatMap(expected -> {
                                                if (paid == null
                                                        || BigDecimal.valueOf(expected).compareTo(paid) != 0) {
                                                    // Summa mos emas — mahsulot faollashtirilmaydi
                                                    return Mono.<Void>empty();
                                                }
                                                return onPaymentSuccess(saved.getMerchantTransId());
                                            });
                                }
                                return Mono.empty();
                            });
                })
                .then();
    }

    private AlifTransactionStatus mapStatus(String status) {
        if (status == null) return AlifTransactionStatus.PENDING;
        return switch (status.toLowerCase()) {
            case "successful" -> AlifTransactionStatus.SUCCESSFUL;
            case "failed"     -> AlifTransactionStatus.FAILED;
            case "expired"    -> AlifTransactionStatus.EXPIRED;
            default           -> AlifTransactionStatus.PENDING;
        };
    }

    /** Prefiksga qarab kutilgan summani (so'm) tanlaydi: kitob yoki kurs. */
    private Mono<Long> expectedAmountSum(String merchantTransId) {
        return merchantTransId != null && merchantTransId.startsWith("course_")
                ? coursePaymentAmountService.expectedAmountSum(merchantTransId)
                : bookPaymentAmountService.expectedAmountSum(merchantTransId);
    }

    /**
     * To'lov muvaffaqiyatida: kitobni faollashtirish yoki kursga avto-enroll
     * (+ checkout token'ni ishlatilgan deb belgilash) — umumiy servis orqali.
     */
    private Mono<Void> onPaymentSuccess(String merchantTransId) {
        return paymentFulfillmentService.onPaymentSuccess(merchantTransId, "alif_nasiya");
    }

    private boolean isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false;
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(authHeader.substring(6)));
            String expected = properties.getShopId() + ":" + properties.getSecretKey();
            return expected.equals(decoded);
        } catch (Exception e) {
            return false;
        }
    }
}
