package uz.sevenEdu.teacherBot.payment.paynet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.service.BooksService;
import uz.sevenEdu.teacherBot.payment.paynet.config.PaynetProperties;
import uz.sevenEdu.teacherBot.payment.paynet.dto.PaynetRequest;
import uz.sevenEdu.teacherBot.payment.paynet.dto.PaynetResponse;
import uz.sevenEdu.teacherBot.payment.paynet.entity.PaynetTransaction;
import uz.sevenEdu.teacherBot.payment.paynet.enums.PaynetTransactionStatus;
import uz.sevenEdu.teacherBot.payment.paynet.repository.PaynetTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Paynet JSON-RPC 2.0 protokoli bo'yicha to'lov qabul qilish.
 * merchantTransId formati: "book_{userId}_{bookId}"
 * amount: tiyin (1 so'm = 100 tiyin)
 */
@Service
@RequiredArgsConstructor
public class PaynetService {

    private final PaynetTransactionRepository paynetTransactionRepository;
    private final PaynetProperties paynetProperties;
    private final BooksService booksService;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Paynet xato kodlari
    private static final int ERR_INSUFFICIENT_PRIVILEGE = 501;
    private static final int ERR_INVALID_AMOUNT         = 411;
    private static final int ERR_TRANSACTION_NOT_FOUND  = 140;
    private static final int ERR_INVALID_ACCOUNT        = 301;
    private static final int ERR_ALREADY_PERFORMED      = 141;
    private static final int ERR_ALREADY_CANCELLED      = 142;
    private static final int ERR_COULD_NOT_CANCEL       = 145;

    public Mono<PaynetResponse> handle(PaynetRequest request, String authHeader) {
        if (!isAuthorized(authHeader)) {
            return Mono.just(PaynetResponse.error(
                    request.getId(), ERR_INSUFFICIENT_PRIVILEGE, "Ruxsat yo'q"));
        }

        return switch (request.getMethod()) {
            case "PerformTransaction" -> performTransaction(request);
            case "CheckTransaction"   -> checkTransaction(request);
            case "CancelTransaction"  -> cancelTransaction(request);
            case "GetStatement"       -> getStatement(request);
            default -> Mono.just(PaynetResponse.error(
                    request.getId(), -32601, "Method not found"));
        };
    }

    // ── PerformTransaction ──────────────────────────────────────────
    private Mono<PaynetResponse> performTransaction(PaynetRequest req) {
        String transactionId  = req.getTransactionId();
        String merchantTransId = req.getMerchantTransId();
        Long amount            = req.getAmount();

        if (merchantTransId == null || !merchantTransId.startsWith("book_")) {
            return Mono.just(PaynetResponse.error(
                    req.getId(), ERR_INVALID_ACCOUNT, "account (order_id) noto'g'ri"));
        }
        if (amount == null || amount <= 0) {
            return Mono.just(PaynetResponse.error(
                    req.getId(), ERR_INVALID_AMOUNT, "Miqdor noto'g'ri"));
        }

        // Duplikat tekshirish
        return paynetTransactionRepository.findByTransactionId(transactionId)
                .flatMap(existing -> {
                    if (existing.getStatus() == PaynetTransactionStatus.SUCCESSFUL) {
                        Map<String, Object> result = txResult(existing, "SUCCESS");
                        return Mono.just(PaynetResponse.ok(req.getId(), result));
                    }
                    if (existing.getStatus() == PaynetTransactionStatus.CANCELLED) {
                        return Mono.just(PaynetResponse.error(
                                req.getId(), ERR_ALREADY_CANCELLED, "Tranzaksiya bekor qilingan"));
                    }
                    // CREATED holatida — qayta perform
                    existing.setStatus(PaynetTransactionStatus.SUCCESSFUL);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return paynetTransactionRepository.save(existing)
                            .flatMap(saved -> onPaymentSuccess(saved.getMerchantTransId())
                                    .thenReturn(PaynetResponse.ok(req.getId(), txResult(saved, "SUCCESS"))));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    PaynetTransaction tx = PaynetTransaction.builder()
                            .transactionId(transactionId)
                            .merchantTransId(merchantTransId)
                            .amount(BigDecimal.valueOf(amount))
                            .status(PaynetTransactionStatus.SUCCESSFUL)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return paynetTransactionRepository.save(tx)
                            .flatMap(saved -> onPaymentSuccess(saved.getMerchantTransId())
                                    .thenReturn(PaynetResponse.ok(req.getId(), txResult(saved, "SUCCESS"))));
                }));
    }

    // ── CheckTransaction ────────────────────────────────────────────
    private Mono<PaynetResponse> checkTransaction(PaynetRequest req) {
        return paynetTransactionRepository.findByTransactionId(req.getTransactionId())
                .map(tx -> PaynetResponse.ok(req.getId(), txResult(tx, statusLabel(tx.getStatus()))))
                .defaultIfEmpty(PaynetResponse.error(
                        req.getId(), ERR_TRANSACTION_NOT_FOUND, "Tranzaksiya topilmadi"));
    }

    // ── CancelTransaction ───────────────────────────────────────────
    private Mono<PaynetResponse> cancelTransaction(PaynetRequest req) {
        return paynetTransactionRepository.findByTransactionId(req.getTransactionId())
                .switchIfEmpty(Mono.error(new RuntimeException("not found")))
                .flatMap(tx -> {
                    if (tx.getStatus() == PaynetTransactionStatus.SUCCESSFUL) {
                        return Mono.just(PaynetResponse.error(
                                req.getId(), ERR_COULD_NOT_CANCEL, "To'langan tranzaksiyani bekor bo'lmaydi"));
                    }
                    if (tx.getStatus() == PaynetTransactionStatus.CANCELLED) {
                        return Mono.just(PaynetResponse.ok(req.getId(), txResult(tx, "CANCELLED")));
                    }
                    tx.setStatus(PaynetTransactionStatus.CANCELLED);
                    tx.setUpdatedAt(LocalDateTime.now());
                    return paynetTransactionRepository.save(tx)
                            .map(saved -> PaynetResponse.ok(req.getId(), txResult(saved, "CANCELLED")));
                })
                .onErrorReturn(PaynetResponse.error(
                        req.getId(), ERR_TRANSACTION_NOT_FOUND, "Tranzaksiya topilmadi"));
    }

    // ── GetStatement ────────────────────────────────────────────────
    private Mono<PaynetResponse> getStatement(PaynetRequest req) {
        String fromStr = req.getDateFrom();
        String toStr   = req.getDateTo();
        LocalDateTime from = fromStr != null ? LocalDateTime.parse(fromStr, DATE_FMT) : LocalDateTime.MIN;
        LocalDateTime to   = toStr   != null ? LocalDateTime.parse(toStr, DATE_FMT)   : LocalDateTime.MAX;

        return paynetTransactionRepository.findByCreatedAtBetween(from, to)
                .map(tx -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("transactionId", tx.getTransactionId());
                    item.put("merchantTransId", tx.getMerchantTransId());
                    item.put("amount", tx.getAmount());
                    item.put("status", tx.getStatus().code);
                    item.put("createdAt", tx.getCreatedAt().format(DATE_FMT));
                    return item;
                })
                .collectList()
                .map(transactions -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("transactions", transactions);
                    return PaynetResponse.ok(req.getId(), result);
                });
    }

    // ── Yordamchi metodlar ──────────────────────────────────────────

    private Mono<Void> onPaymentSuccess(String merchantTransId) {
        if (merchantTransId == null) return Mono.empty();
        String[] parts = merchantTransId.split("_");
        if (parts.length == 3 && "book".equals(parts[0])) {
            try {
                Long userId = Long.parseLong(parts[1]);
                Long bookId = Long.parseLong(parts[2]);
                return booksService.purchaseBook(userId, bookId, "paynet", merchantTransId);
            } catch (NumberFormatException ignored) {}
        }
        return Mono.empty();
    }

    private Map<String, Object> txResult(PaynetTransaction tx, String statusLabel) {
        Map<String, Object> result = new HashMap<>();
        result.put("transactionId", tx.getTransactionId());
        result.put("transaction", tx.getId().toString());
        result.put("amount", tx.getAmount());
        result.put("status", tx.getStatus().code);
        result.put("statusLabel", statusLabel);
        result.put("createdAt", tx.getCreatedAt().format(DATE_FMT));
        return result;
    }

    private String statusLabel(PaynetTransactionStatus status) {
        return switch (status) {
            case CREATED    -> "CREATED";
            case SUCCESSFUL -> "SUCCESS";
            case CANCELLED  -> "CANCELLED";
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
        String expected = paynetProperties.getUsername() + ":" + paynetProperties.getPassword();
        return expected.equals(decoded);
    }
}
