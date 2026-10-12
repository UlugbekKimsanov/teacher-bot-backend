package uz.sevenEdu.teacherBot.payment.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.common.security.RoleGuard;
import uz.sevenEdu.teacherBot.payment.click.config.ClickProperties;
import uz.sevenEdu.teacherBot.payment.order.entity.PaymentOrder;
import uz.sevenEdu.teacherBot.payment.order.repository.PaymentOrderRepository;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentOrderController {

    private final PaymentOrderRepository orderRepository;
    private final ClickProperties clickProperties;
    private final RoleGuard roleGuard;

    /**
     * Yangi buyurtma yaratadi va Click payment URL qaytaradi.
     * Auth majburiy; userId clientdan emas — tokendan olinadi
     * (eski "userId" parametri kontrakt uchun qabul qilinadi, lekin e'tiborga olinmaydi).
     */
    @PostMapping("/order")
    public Mono<ApiResponse<Map<String, Object>>> createOrder(
            @RequestParam BigDecimal amount,
            @RequestParam Long productId,
            @RequestParam String productType,
            @RequestParam(name = "userId", required = false) Long ignoredUserId,
            Authentication auth) {

        Long userId = requireUserId(auth);

        PaymentOrder order = PaymentOrder.builder()
                .userId(userId)
                .productId(productId)
                .productType(productType.toUpperCase())
                .amount(amount)
                .status("PENDING")
                .build();

        return orderRepository.save(order)
                .map(saved -> {
                    String clickUrl = "https://my.click.uz/services/pay/"
                            + "?service_id=" + clickProperties.getServiceId()
                            + "&merchant_id=" + clickProperties.getMerchantId()
                            + "&amount=" + saved.getAmount().toPlainString()
                            + "&transaction_param=" + saved.getId();

                    return ApiResponse.ok(Map.of(
                            "orderId", saved.getId(),
                            "productId", saved.getProductId(),
                            "productType", saved.getProductType(),
                            "amount", saved.getAmount(),
                            "status", saved.getStatus(),
                            "clickUrl", clickUrl
                    ));
                });
    }

    /**
     * Buyurtma holatini tekshirish — faqat buyurtma egasi yoki admin ko'radi.
     */
    @GetMapping("/order/{id}")
    public Mono<ApiResponse<PaymentOrder>> getOrder(@PathVariable Long id, Authentication auth) {
        Long userId = requireUserId(auth);
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Buyurtma topilmadi")))
                .flatMap(order -> {
                    if (order.getUserId() != null && order.getUserId().equals(userId)) {
                        return Mono.just(ApiResponse.ok(order));
                    }
                    return roleGuard.isAdmin(userId)
                            .flatMap(admin -> admin
                                    ? Mono.just(ApiResponse.ok(order))
                                    : Mono.error(new ForbiddenException("Bu buyurtma sizga tegishli emas")));
                });
    }

    /** Token yo'q/yaroqsiz bo'lsa 401. */
    private Long requireUserId(Authentication auth) {
        if (auth == null) throw new UnauthorizedException("Tizimga kirish talab etiladi");
        return (Long) auth.getPrincipal();
    }
}
