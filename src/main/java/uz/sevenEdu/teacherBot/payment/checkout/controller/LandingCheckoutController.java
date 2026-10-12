package uz.sevenEdu.teacherBot.payment.checkout.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.payment.checkout.dto.CheckoutInfoDto;
import uz.sevenEdu.teacherBot.payment.checkout.dto.CheckoutPayRequest;
import uz.sevenEdu.teacherBot.payment.checkout.service.CheckoutLinkService;

import java.util.Map;

/**
 * Public landing checkout endpointlari (avtorizatsiyasiz, /api/landing ostida).
 * Token o'zi autentifikatsiya vazifasini bajaradi (qisqa muddatli, 256-bit).
 */
@RestController
@RequestMapping("/api/landing/checkout")
@RequiredArgsConstructor
public class LandingCheckoutController {

    private final CheckoutLinkService checkoutLinkService;

    /**
     * Checkout sahifasi ma'lumoti — FAQAT O'QISH (hech narsa consume qilinmaydi).
     * Token topilmasa/ishlatilgan/muddati o'tgan bo'lsa success=false xabar qaytadi.
     */
    @GetMapping("/{token}")
    public Mono<ApiResponse<CheckoutInfoDto>> info(@PathVariable String token) {
        return checkoutLinkService.getCheckoutInfo(token).map(ApiResponse::ok);
    }

    /**
     * Tanlangan usul bo'yicha provayder checkout URL'ini quradi.
     * Body: { "method": "click" | "payme" | "paynet" | "uzumnasiya" | "alifnasiya" }
     * Javob data: { "checkoutUrl": "https://..." }
     */
    @PostMapping("/{token}/pay")
    public Mono<ApiResponse<Map<String, String>>> pay(
            @PathVariable String token, @RequestBody CheckoutPayRequest request) {
        return checkoutLinkService.pay(token, request.getMethod()).map(ApiResponse::ok);
    }
}
