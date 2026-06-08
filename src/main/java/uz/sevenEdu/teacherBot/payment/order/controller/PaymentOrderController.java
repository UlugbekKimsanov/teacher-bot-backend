package uz.sevenEdu.teacherBot.payment.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
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

    /**
     * Yangi buyurtma yaratadi va Click payment URL qaytaradi.
     */
    @PostMapping("/order")
    public Mono<ApiResponse<Map<String, Object>>> createOrder(
            @RequestParam BigDecimal amount,
            @RequestParam Long productId,
            @RequestParam String productType,
            @RequestParam(defaultValue = "0") Long userId) {

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
     * Buyurtma holatini tekshirish.
     */
    @GetMapping("/order/{id}")
    public Mono<ApiResponse<PaymentOrder>> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ApiResponse::ok)
                .switchIfEmpty(Mono.error(new RuntimeException("Buyurtma topilmadi")));
    }
}
