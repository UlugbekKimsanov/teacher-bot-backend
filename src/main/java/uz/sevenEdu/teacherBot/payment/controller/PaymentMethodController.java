package uz.sevenEdu.teacherBot.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.payment.entity.PaymentMethod;
import uz.sevenEdu.teacherBot.payment.repository.PaymentMethodRepository;

import java.util.List;

/** Mobil uchun — faqat yoqilgan (enabled) to'lov usullari. */
@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodRepository paymentMethodRepository;

    @GetMapping
    public Mono<ApiResponse<List<PaymentMethod>>> getEnabled() {
        return paymentMethodRepository.findByEnabledTrueOrderByOrderIndexAsc()
                .collectList()
                .map(ApiResponse::ok);
    }
}
