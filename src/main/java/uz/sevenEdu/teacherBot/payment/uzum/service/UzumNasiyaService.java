package uz.sevenEdu.teacherBot.payment.uzum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.common.BookPaymentAmountService;
import uz.sevenEdu.teacherBot.payment.common.CoursePaymentAmountService;
import uz.sevenEdu.teacherBot.payment.common.PaymentFulfillmentService;
import uz.sevenEdu.teacherBot.payment.uzum.config.UzumNasiyaProperties;
import uz.sevenEdu.teacherBot.payment.uzum.dto.UzumNasiyaRequest;
import uz.sevenEdu.teacherBot.payment.uzum.dto.UzumNasiyaResponse;
import uz.sevenEdu.teacherBot.payment.uzum.entity.UzumNasiyaTransaction;
import uz.sevenEdu.teacherBot.payment.uzum.enums.UzumNasiyaTransactionStatus;
import uz.sevenEdu.teacherBot.payment.uzum.repository.UzumNasiyaTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UzumNasiyaService {

    private final UzumNasiyaTransactionRepository repository;
    private final UzumNasiyaProperties properties;
    private final BookPaymentAmountService bookPaymentAmountService;
    private final CoursePaymentAmountService coursePaymentAmountService;
    private final PaymentFulfillmentService paymentFulfillmentService;

    // ── Check ───────────────────────────────────────────────────────
    public Mono<UzumNasiyaResponse> check(UzumNasiyaRequest req) {
        if (req.getServiceId() != properties.getServiceId()) {
            return Mono.just(UzumNasiyaResponse.error(-1, "serviceId noto'g'ri"));
        }
        String merchantTransId = req.getMerchantTransId();
        if (!isSupportedMerchantTransId(merchantTransId)) {
            return Mono.just(UzumNasiyaResponse.error(-2, "order_id noto'g'ri"));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("merchantTransId", merchantTransId);
        data.put("allow", true);
        return Mono.just(UzumNasiyaResponse.ok(data));
    }

    // ── Create ──────────────────────────────────────────────────────
    public Mono<UzumNasiyaResponse> create(UzumNasiyaRequest req) {
        if (req.getServiceId() != properties.getServiceId()) {
            return Mono.just(UzumNasiyaResponse.error(-1, "serviceId noto'g'ri"));
        }
        String transId = req.getTransId();
        String merchantTransId = req.getMerchantTransId();
        Long amount = req.getAmount();

        if (!isSupportedMerchantTransId(merchantTransId)) {
            return Mono.just(UzumNasiyaResponse.error(-2, "order_id noto'g'ri"));
        }
        if (amount == null || amount <= 0) {
            return Mono.just(UzumNasiyaResponse.error(-3, "Miqdor noto'g'ri"));
        }

        // Duplikat tekshirish
        return repository.findByTransId(transId)
                .flatMap(existing -> {
                    Map<String, Object> data = txData(existing);
                    return Mono.just(UzumNasiyaResponse.ok(data));
                })
                .switchIfEmpty(Mono.defer(() ->
                        // Yangi tranzaksiya — avval summa (so'm) kutilgan mahsulot (kitob/kurs)
                        // narxi bilan solishtiriladi
                        expectedAmountSum(merchantTransId)
                                .flatMap(expected -> {
                                    if (!expected.equals(amount)) {
                                        return Mono.just(UzumNasiyaResponse.error(-3,
                                                "Miqdor mahsulot narxiga mos emas"));
                                    }
                                    UzumNasiyaTransaction tx = UzumNasiyaTransaction.builder()
                                            .transId(transId)
                                            .merchantTransId(merchantTransId)
                                            .amount(BigDecimal.valueOf(amount))
                                            .status(UzumNasiyaTransactionStatus.PENDING)
                                            .createdAt(LocalDateTime.now())
                                            .updatedAt(LocalDateTime.now())
                                            .build();
                                    return repository.save(tx)
                                            .map(saved -> UzumNasiyaResponse.ok(txData(saved)));
                                })
                                .switchIfEmpty(Mono.just(UzumNasiyaResponse.error(-2, "Mahsulot topilmadi")))
                ));
    }

    // ── Confirm ─────────────────────────────────────────────────────
    public Mono<UzumNasiyaResponse> confirm(UzumNasiyaRequest req) {
        if (req.getServiceId() != properties.getServiceId()) {
            return Mono.just(UzumNasiyaResponse.error(-1, "serviceId noto'g'ri"));
        }
        return repository.findByTransId(req.getTransId())
                .switchIfEmpty(Mono.error(new RuntimeException("not found")))
                .flatMap(tx -> {
                    if (tx.getStatus() == UzumNasiyaTransactionStatus.PAID) {
                        return Mono.just(UzumNasiyaResponse.ok(txData(tx)));
                    }
                    if (tx.getStatus() == UzumNasiyaTransactionStatus.CANCELED) {
                        return Mono.just(UzumNasiyaResponse.error(-5, "Tranzaksiya bekor qilingan"));
                    }
                    tx.setStatus(UzumNasiyaTransactionStatus.PAID);
                    tx.setUpdatedAt(LocalDateTime.now());
                    return repository.save(tx)
                            .flatMap(saved -> onPaymentSuccess(saved.getMerchantTransId())
                                    .thenReturn(UzumNasiyaResponse.ok(txData(saved))));
                })
                .onErrorReturn(UzumNasiyaResponse.error(-4, "Tranzaksiya topilmadi"));
    }

    // ── Reverse ─────────────────────────────────────────────────────
    public Mono<UzumNasiyaResponse> reverse(UzumNasiyaRequest req) {
        if (req.getServiceId() != properties.getServiceId()) {
            return Mono.just(UzumNasiyaResponse.error(-1, "serviceId noto'g'ri"));
        }
        return repository.findByTransId(req.getTransId())
                .switchIfEmpty(Mono.error(new RuntimeException("not found")))
                .flatMap(tx -> {
                    if (tx.getStatus() == UzumNasiyaTransactionStatus.CANCELED) {
                        return Mono.just(UzumNasiyaResponse.ok(txData(tx)));
                    }
                    if (tx.getStatus() == UzumNasiyaTransactionStatus.PAID) {
                        return Mono.just(UzumNasiyaResponse.error(-6,
                                "To'langan tranzaksiyani bekor bo'lmaydi"));
                    }
                    tx.setStatus(UzumNasiyaTransactionStatus.CANCELED);
                    tx.setUpdatedAt(LocalDateTime.now());
                    return repository.save(tx)
                            .map(saved -> UzumNasiyaResponse.ok(txData(saved)));
                })
                .onErrorReturn(UzumNasiyaResponse.error(-4, "Tranzaksiya topilmadi"));
    }

    // ── Status ──────────────────────────────────────────────────────
    public Mono<UzumNasiyaResponse> status(UzumNasiyaRequest req) {
        if (req.getServiceId() != properties.getServiceId()) {
            return Mono.just(UzumNasiyaResponse.error(-1, "serviceId noto'g'ri"));
        }
        return repository.findByTransId(req.getTransId())
                .map(tx -> UzumNasiyaResponse.ok(txData(tx)))
                .defaultIfEmpty(UzumNasiyaResponse.error(-4, "Tranzaksiya topilmadi"));
    }

    // ── Yordamchi ───────────────────────────────────────────────────

    /** Qo'llab-quvvatlanadigan merchantTransId prefikslari: book_ va course_ */
    private boolean isSupportedMerchantTransId(String merchantTransId) {
        return merchantTransId != null
                && (merchantTransId.startsWith("book_") || merchantTransId.startsWith("course_"));
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
        return paymentFulfillmentService.onPaymentSuccess(merchantTransId, "uzum_nasiya");
    }

    private Map<String, Object> txData(UzumNasiyaTransaction tx) {
        Map<String, Object> data = new HashMap<>();
        data.put("transId", tx.getTransId());
        data.put("merchantTransId", tx.getMerchantTransId());
        data.put("amount", tx.getAmount());
        data.put("status", tx.getStatus().name());
        return data;
    }

    public boolean isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false;
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(authHeader.substring(6)));
            String expected = properties.getUsername() + ":" + properties.getPassword();
            return expected.equals(decoded);
        } catch (Exception e) {
            return false;
        }
    }
}
