package uz.sevenEdu.teacherBot.payment.checkout.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.ForbiddenException;
import uz.sevenEdu.teacherBot.common.exception.UnauthorizedException;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.common.util.GuestUtil;
import uz.sevenEdu.teacherBot.payment.checkout.dto.CheckoutLinkDto;
import uz.sevenEdu.teacherBot.payment.checkout.dto.CheckoutLinkRequest;
import uz.sevenEdu.teacherBot.payment.checkout.service.CheckoutLinkService;

/**
 * Mobil ilova uchun: web checkout havolasi (token) yaratish.
 * Bearer auth SHART — mehmon hisobi bilan xarid taqiqlanadi.
 */
@RestController
@RequestMapping("/api/v1/checkout-links")
@RequiredArgsConstructor
public class CheckoutLinkController {

    private final CheckoutLinkService checkoutLinkService;

    /**
     * Body: { "productType": "course"|"book", "productId": 1 }
     * Javob data: { "url": "https://oazisedu.uz/?t=...", "token": "...", "expiresAt": "..." }
     */
    @PostMapping
    public Mono<ApiResponse<CheckoutLinkDto>> create(
            @RequestBody CheckoutLinkRequest request, Authentication auth) {
        if (auth == null) {
            return Mono.error(new UnauthorizedException("Tizimga kirish talab etiladi"));
        }
        Long userId = (Long) auth.getPrincipal();
        if (GuestUtil.isGuest(userId)) {
            return Mono.error(new ForbiddenException("Mehmon hisobi bilan xarid qilib bo'lmaydi"));
        }
        return checkoutLinkService.createLink(userId, request).map(ApiResponse::ok);
    }
}
