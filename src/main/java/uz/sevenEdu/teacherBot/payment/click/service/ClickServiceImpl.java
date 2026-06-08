package uz.sevenEdu.teacherBot.payment.click.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.service.BooksService;
import uz.sevenEdu.teacherBot.payment.order.repository.PaymentOrderRepository;
import uz.sevenEdu.teacherBot.payment.click.config.ClickProperties;
import uz.sevenEdu.teacherBot.payment.click.dto.ClickCompleteRequestDto;
import uz.sevenEdu.teacherBot.payment.click.dto.ClickPrepareRequestDto;
import uz.sevenEdu.teacherBot.payment.click.dto.ClickResponseDto;
import uz.sevenEdu.teacherBot.payment.click.entity.ClickTransaction;
import uz.sevenEdu.teacherBot.payment.click.enums.ClickError;
import uz.sevenEdu.teacherBot.payment.click.enums.ClickTransactionStatus;
import uz.sevenEdu.teacherBot.payment.click.repository.ClickTransactionRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClickServiceImpl implements ClickService {

    private final ClickProperties clickProperties;
    private final ClickTransactionRepository clickTransactionRepository;
    private final BooksService booksService;
    private final PaymentOrderRepository paymentOrderRepository;

    @Override
    public Mono<ClickResponseDto> prepare(ClickPrepareRequestDto request) {
        // 1. Sign tekshirish
        String expectedSign = md5(
            request.getClickTransId() + String.valueOf(request.getServiceId()) +
            clickProperties.getSecretKey() + request.getMerchantTransId() +
            request.getAmount() + request.getAction() + request.getSignTime()
        );
        if (!expectedSign.equals(request.getSignString())) {
            return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), null, ClickError.SIGN_CHECK_FAILED));
        }

        // 2. Service ID tekshirish
        if (!clickProperties.getServiceId().equals(request.getServiceId())) {
            return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), null, ClickError.SIGN_CHECK_FAILED));
        }

        // 3. merchant_trans_id (order ID) mavjudligini va holatini tekshirish
        Long orderId;
        try {
            orderId = Long.parseLong(request.getMerchantTransId());
        } catch (NumberFormatException e) {
            return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), null, ClickError.USER_NOT_FOUND));
        }

        return paymentOrderRepository.findById(orderId)
            .flatMap(order -> {
                // 4. To'langan bo'lsa xato
                if ("PAID".equals(order.getStatus())) {
                    return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), null, ClickError.ALREADY_PAID));
                }

                // 5. Miqdor tekshirish
                if (order.getAmount().compareTo(request.getAmount()) != 0) {
                    return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), null, ClickError.INCORRECT_AMOUNT));
                }

                // 6. Mavjud click tranzaksiyani tekshirish (qayta prepare kelsa)
                return clickTransactionRepository.findByMerchantTransId(request.getMerchantTransId())
                    .flatMap(existing -> {
                        if (existing.getStatus() == ClickTransactionStatus.PAID) {
                            return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), existing.getId(), ClickError.ALREADY_PAID));
                        }
                        return Mono.just(ClickResponseDto.builder()
                            .clickTransId(request.getClickTransId())
                            .merchantTransId(request.getMerchantTransId())
                            .merchantPrepareId(existing.getId())
                            .merchantConfirmId(existing.getId())
                            .error(ClickError.SUCCESS.getCode())
                            .errorNote(ClickError.SUCCESS.getMessage())
                            .build());
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        // 7. Yangi click tranzaksiya saqlash
                        ClickTransaction transaction = ClickTransaction.builder()
                            .clickTransId(request.getClickTransId())
                            .clickPaydocId(request.getClickPaydocId())
                            .merchantTransId(request.getMerchantTransId())
                            .amount(request.getAmount())
                            .status(ClickTransactionStatus.PREPARED)
                            .signTime(request.getSignTime())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                        return clickTransactionRepository.save(transaction)
                            .map(saved -> ClickResponseDto.builder()
                                .clickTransId(request.getClickTransId())
                                .merchantTransId(request.getMerchantTransId())
                                .merchantPrepareId(saved.getId())
                                .merchantConfirmId(saved.getId())
                                .error(ClickError.SUCCESS.getCode())
                                .errorNote(ClickError.SUCCESS.getMessage())
                                .build());
                    }));
            })
            .switchIfEmpty(Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), null, ClickError.USER_NOT_FOUND)));
    }

    @Override
    public Mono<ClickResponseDto> complete(ClickCompleteRequestDto request) {
        // 1. Sign tekshirish
        // MD5(click_trans_id + service_id + secret_key + merchant_trans_id + merchant_prepare_id + amount + action + sign_time)
        String expectedSign = md5(
            request.getClickTransId() + String.valueOf(request.getServiceId()) +
            clickProperties.getSecretKey() + request.getMerchantTransId() +
            request.getMerchantPrepareId() + request.getAmount() +
            request.getAction() + request.getSignTime()
        );
        if (!expectedSign.equals(request.getSignString())) {
            return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), request.getMerchantPrepareId(), ClickError.SIGN_CHECK_FAILED));
        }

        if (request.getMerchantPrepareId() == null) {
            return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), null, ClickError.TRANSACTION_NOT_FOUND));
        }

        return clickTransactionRepository.findById(request.getMerchantPrepareId())
            .flatMap(transaction -> {
                // 2. Click xatoligi — bekor qilish
                if (request.getError() != null && request.getError() < 0) {
                    transaction.setStatus(ClickTransactionStatus.CANCELLED);
                    transaction.setUpdatedAt(LocalDateTime.now());
                    return clickTransactionRepository.save(transaction)
                        .thenReturn(errorResponse(request.getClickTransId(), request.getMerchantTransId(), request.getMerchantPrepareId(), ClickError.TRANSACTION_CANCELLED));
                }

                // 3. Allaqachon to'langan
                if (transaction.getStatus() == ClickTransactionStatus.PAID) {
                    return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), request.getMerchantPrepareId(), ClickError.ALREADY_PAID));
                }

                // 4. Miqdor tekshirish
                if (transaction.getAmount().compareTo(request.getAmount()) != 0) {
                    return Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), request.getMerchantPrepareId(), ClickError.INCORRECT_AMOUNT));
                }

                // 5. To'lov yakunlash
                transaction.setStatus(ClickTransactionStatus.PAID);
                transaction.setUpdatedAt(LocalDateTime.now());
                return clickTransactionRepository.save(transaction)
                    .flatMap(saved -> onPaymentSuccess(saved.getMerchantTransId(), saved.getAmount())
                        .thenReturn(ClickResponseDto.builder()
                            .clickTransId(request.getClickTransId())
                            .merchantTransId(request.getMerchantTransId())
                            .merchantPrepareId(request.getMerchantPrepareId())
                            .merchantConfirmId(saved.getId())
                            .error(ClickError.SUCCESS.getCode())
                            .errorNote(ClickError.SUCCESS.getMessage())
                            .build()));
            })
            .switchIfEmpty(Mono.just(errorResponse(request.getClickTransId(), request.getMerchantTransId(), request.getMerchantPrepareId(), ClickError.TRANSACTION_NOT_FOUND)));
    }

    /**
     * To'lov muvaffaqiyatli bo'lganda chaqiriladi.
     * merchantTransId formati: "book_{userId}_{bookId}"
     */
    private Mono<Void> onPaymentSuccess(String merchantTransId, BigDecimal amount) {
        if (merchantTransId == null) return Mono.empty();

        // book_{userId}_{bookId} formati
        String[] parts = merchantTransId.split("_");
        if (parts.length == 3 && "book".equals(parts[0])) {
            try {
                Long userId = Long.parseLong(parts[1]);
                Long bookId = Long.parseLong(parts[2]);
                return booksService.purchaseBook(userId, bookId, "click", merchantTransId);
            } catch (NumberFormatException ignored) {}
        }

        // Order ID (raqam)
        try {
            Long orderId = Long.parseLong(merchantTransId);
            return paymentOrderRepository.findById(orderId)
                .flatMap(order -> {
                    order.setStatus("PAID");
                    order.setPaymentMethod("click");
                    return paymentOrderRepository.save(order);
                })
                .then();
        } catch (NumberFormatException ignored) {}

        return Mono.empty();
    }

    private ClickResponseDto errorResponse(Long clickTransId, String merchantTransId, Long merchantPrepareId, ClickError error) {
        return ClickResponseDto.builder()
            .clickTransId(clickTransId)
            .merchantTransId(merchantTransId)
            .merchantPrepareId(merchantPrepareId)
            .merchantConfirmId(merchantPrepareId)
            .error(error.getCode())
            .errorNote(error.getMessage())
            .build();
    }

    private String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}
