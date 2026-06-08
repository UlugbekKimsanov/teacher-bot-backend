package uz.sevenEdu.teacherBot.payment.payme.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.service.BooksService;
import uz.sevenEdu.teacherBot.payment.payme.config.PaymeProperties;
import uz.sevenEdu.teacherBot.payment.payme.dto.PaymeRequest;
import uz.sevenEdu.teacherBot.payment.payme.dto.PaymeResponse;
import uz.sevenEdu.teacherBot.payment.payme.entity.PaymeTransaction;
import uz.sevenEdu.teacherBot.payment.payme.enums.PaymeTransactionStatus;
import uz.sevenEdu.teacherBot.payment.payme.repository.PaymeTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Payme JSON-RPC 2.0 protokoli bo'yicha to'lov qabul qilish.
 * https://developer.help.paycom.uz/
 *
 * merchantTransId formati: "book_{userId}_{bookId}"
 * amount: tiyin (1 so'm = 100 tiyin)
 */
@Service
@RequiredArgsConstructor
public class PaymeService {

    private final PaymeTransactionRepository paymeTransactionRepository;
    private final PaymeProperties paymeProperties;
    private final BooksService booksService;

    // ── Payme xato kodlari ──────────────────────────────────────────
    private static final int ERR_INSUFFICIENT_PRIVILEGE = -32504;
    private static final int ERR_INVALID_AMOUNT         = -31001;
    private static final int ERR_TRANSACTION_NOT_FOUND  = -31003;
    private static final int ERR_INVALID_ACCOUNT        = -31050;
    private static final int ERR_COULD_NOT_PERFORM      = -31008;
    private static final int ERR_COULD_NOT_CANCEL       = -31007;

    public Mono<PaymeResponse> handle(PaymeRequest request, String authHeader) {
        // Basic auth tekshirish: "Basic base64(Paycom:{secretKey})"
        if (!isAuthorized(authHeader)) {
            return Mono.just(PaymeResponse.error(
                    request.getId(), ERR_INSUFFICIENT_PRIVILEGE, "Ruxsat yo'q"));
        }

        return switch (request.getMethod()) {
            case "CheckPerformTransaction"  -> checkPerformTransaction(request);
            case "CreateTransaction"        -> createTransaction(request);
            case "PerformTransaction"       -> performTransaction(request);
            case "CancelTransaction"        -> cancelTransaction(request);
            case "CheckTransaction"         -> checkTransaction(request);
            case "GetStatement"             -> getStatement(request);
            default -> Mono.just(PaymeResponse.error(
                    request.getId(), -32601, "Method not found"));
        };
    }

    // ── CheckPerformTransaction ─────────────────────────────────────
    private Mono<PaymeResponse> checkPerformTransaction(PaymeRequest req) {
        String merchantTransId = req.getMerchantTransId();
        Long amount = req.getAmount();

        if (merchantTransId == null || !merchantTransId.startsWith("book_")) {
            return Mono.just(PaymeResponse.error(
                    req.getId(), ERR_INVALID_ACCOUNT, "account.order_id noto'g'ri"));
        }
        if (amount == null || amount <= 0) {
            return Mono.just(PaymeResponse.error(
                    req.getId(), ERR_INVALID_AMOUNT, "Miqdor noto'g'ri"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("allow", true);
        return Mono.just(PaymeResponse.ok(req.getId(), result));
    }

    // ── CreateTransaction ───────────────────────────────────────────
    private Mono<PaymeResponse> createTransaction(PaymeRequest req) {
        String paymeId = req.getPaymeTransId();
        String merchantTransId = req.getMerchantTransId();
        Long amount = req.getAmount();
        Long createTime = req.getCreateTime();

        if (merchantTransId == null || !merchantTransId.startsWith("book_")) {
            return Mono.just(PaymeResponse.error(
                    req.getId(), ERR_INVALID_ACCOUNT, "account.order_id noto'g'ri"));
        }

        return paymeTransactionRepository.findByPaymeId(paymeId)
                .flatMap(existing -> {
                    // Tranzaksiya allaqachon mavjud
                    if (existing.getStatus() == PaymeTransactionStatus.CANCELLED) {
                        return Mono.just(PaymeResponse.error(
                                req.getId(), ERR_COULD_NOT_PERFORM, "Tranzaksiya bekor qilingan"));
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("create_time", existing.getCreateTime());
                    result.put("transaction", existing.getId().toString());
                    result.put("state", stateOf(existing.getStatus()));
                    return Mono.just(PaymeResponse.ok(req.getId(), result));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Yangi tranzaksiya yaratish
                    PaymeTransaction tx = PaymeTransaction.builder()
                            .paymeId(paymeId)
                            .merchantTransId(merchantTransId)
                            .amount(BigDecimal.valueOf(amount))
                            .status(PaymeTransactionStatus.CREATED)
                            .createTime(createTime)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return paymeTransactionRepository.save(tx)
                            .map(saved -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("create_time", saved.getCreateTime());
                                result.put("transaction", saved.getId().toString());
                                result.put("state", 1); // CREATED
                                return PaymeResponse.ok(req.getId(), result);
                            });
                }));
    }

    // ── PerformTransaction ──────────────────────────────────────────
    private Mono<PaymeResponse> performTransaction(PaymeRequest req) {
        String paymeId = req.getPaymeTransId();
        long performTime = System.currentTimeMillis();

        return paymeTransactionRepository.findByPaymeId(paymeId)
                .switchIfEmpty(Mono.error(new RuntimeException("not found")))
                .flatMap(tx -> {
                    if (tx.getStatus() == PaymeTransactionStatus.PERFORMED) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("perform_time", tx.getPerformTime());
                        result.put("transaction", tx.getId().toString());
                        result.put("state", 2);
                        return Mono.just(PaymeResponse.ok(req.getId(), result));
                    }
                    if (tx.getStatus() == PaymeTransactionStatus.CANCELLED) {
                        return Mono.just(PaymeResponse.error(
                                req.getId(), ERR_COULD_NOT_PERFORM, "Tranzaksiya bekor qilingan"));
                    }
                    tx.setStatus(PaymeTransactionStatus.PERFORMED);
                    tx.setPerformTime(performTime);
                    tx.setUpdatedAt(LocalDateTime.now());
                    return paymeTransactionRepository.save(tx)
                            .flatMap(saved -> onPaymentSuccess(saved.getMerchantTransId())
                                    .thenReturn(saved))
                            .map(saved -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("perform_time", saved.getPerformTime());
                                result.put("transaction", saved.getId().toString());
                                result.put("state", 2);
                                return PaymeResponse.ok(req.getId(), result);
                            });
                })
                .onErrorReturn(PaymeResponse.error(
                        req.getId(), ERR_TRANSACTION_NOT_FOUND, "Tranzaksiya topilmadi"));
    }

    // ── CancelTransaction ───────────────────────────────────────────
    private Mono<PaymeResponse> cancelTransaction(PaymeRequest req) {
        String paymeId = req.getPaymeTransId();
        Integer reason = req.getCancelReason();
        long cancelTime = System.currentTimeMillis();

        return paymeTransactionRepository.findByPaymeId(paymeId)
                .switchIfEmpty(Mono.error(new RuntimeException("not found")))
                .flatMap(tx -> {
                    if (tx.getStatus() == PaymeTransactionStatus.PERFORMED) {
                        return Mono.just(PaymeResponse.error(
                                req.getId(), ERR_COULD_NOT_CANCEL, "To'langan tranzaksiyani bekor bo'lmaydi"));
                    }
                    if (tx.getStatus() == PaymeTransactionStatus.CANCELLED) {
                        Map<String, Object> result = new HashMap<>();
                        result.put("cancel_time", tx.getCancelTime());
                        result.put("transaction", tx.getId().toString());
                        result.put("state", -1);
                        return Mono.just(PaymeResponse.ok(req.getId(), result));
                    }
                    tx.setStatus(PaymeTransactionStatus.CANCELLED);
                    tx.setCancelTime(cancelTime);
                    tx.setCancelReason(reason);
                    tx.setUpdatedAt(LocalDateTime.now());
                    return paymeTransactionRepository.save(tx)
                            .map(saved -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("cancel_time", saved.getCancelTime());
                                result.put("transaction", saved.getId().toString());
                                result.put("state", -1);
                                return PaymeResponse.ok(req.getId(), result);
                            });
                })
                .onErrorReturn(PaymeResponse.error(
                        req.getId(), ERR_TRANSACTION_NOT_FOUND, "Tranzaksiya topilmadi"));
    }

    // ── CheckTransaction ────────────────────────────────────────────
    private Mono<PaymeResponse> checkTransaction(PaymeRequest req) {
        String paymeId = req.getPaymeTransId();
        return paymeTransactionRepository.findByPaymeId(paymeId)
                .map(tx -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("create_time", tx.getCreateTime());
                    result.put("perform_time", tx.getPerformTime() != null ? tx.getPerformTime() : 0);
                    result.put("cancel_time", tx.getCancelTime() != null ? tx.getCancelTime() : 0);
                    result.put("transaction", tx.getId().toString());
                    result.put("state", stateOf(tx.getStatus()));
                    result.put("reason", tx.getCancelReason());
                    return PaymeResponse.ok(req.getId(), result);
                })
                .defaultIfEmpty(PaymeResponse.error(
                        req.getId(), ERR_TRANSACTION_NOT_FOUND, "Tranzaksiya topilmadi"));
    }

    // ── GetStatement ────────────────────────────────────────────────
    private Mono<PaymeResponse> getStatement(PaymeRequest req) {
        Long from = req.getFrom();
        Long to = req.getTo();
        return paymeTransactionRepository.findByCreateTimeBetween(from, to)
                .map(tx -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", tx.getPaymeId());
                    item.put("time", tx.getCreateTime());
                    item.put("amount", tx.getAmount());
                    item.put("account", Map.of("order_id", tx.getMerchantTransId()));
                    item.put("create_time", tx.getCreateTime());
                    item.put("perform_time", tx.getPerformTime() != null ? tx.getPerformTime() : 0);
                    item.put("cancel_time", tx.getCancelTime() != null ? tx.getCancelTime() : 0);
                    item.put("transaction", tx.getId().toString());
                    item.put("state", stateOf(tx.getStatus()));
                    item.put("reason", tx.getCancelReason());
                    return item;
                })
                .collectList()
                .map(transactions -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("transactions", transactions);
                    return PaymeResponse.ok(req.getId(), result);
                });
    }

    // ── Yordamchi metodlar ──────────────────────────────────────────

    /**
     * merchantTransId bo'yicha user va kitobni topib purchaseBook chaqiradi.
     * format: "book_{userId}_{bookId}"
     */
    private Mono<Void> onPaymentSuccess(String merchantTransId) {
        if (merchantTransId == null) return Mono.empty();
        String[] parts = merchantTransId.split("_");
        if (parts.length == 3 && "book".equals(parts[0])) {
            try {
                Long userId = Long.parseLong(parts[1]);
                Long bookId = Long.parseLong(parts[2]);
                return booksService.purchaseBook(userId, bookId, "payme", merchantTransId);
            } catch (NumberFormatException ignored) {}
        }
        return Mono.empty();
    }

    /** Payme state kodi: 1=created, 2=performed, -1=cancelled */
    private int stateOf(PaymeTransactionStatus status) {
        return switch (status) {
            case CREATED, PERFORMING -> 1;
            case PERFORMED           -> 2;
            case CANCELLED           -> -1;
        };
    }

    private boolean isAuthorized(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) return false;
        String encoded = authHeader.substring(6);
        String decoded;
        try {
            decoded = new String(java.util.Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            return false;
        }
        // Payme format: "Paycom:{secretKey}"
        String expected = "Paycom:" + paymeProperties.getSecretKey();
        return expected.equals(decoded);
    }
}
