package uz.sevenEdu.teacherBot.landing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.landing.dto.LandingContentDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadDto;
import uz.sevenEdu.teacherBot.landing.dto.LeadRequest;
import uz.sevenEdu.teacherBot.landing.service.LandingService;

/** Public landing endpointlari (avtorizatsiyasiz). */
@RestController
@RequestMapping("/api/landing")
@RequiredArgsConstructor
public class LandingController {

    private final LandingService landingService;

    /** Landing sahifa kontenti — bitta JSON obyekt. */
    @GetMapping
    public Mono<ApiResponse<LandingContentDto>> getContent() {
        return landingService.getContent().map(ApiResponse::ok);
    }

    /** Ro'yxatdan o'tish (lead) — faqat DB ga yozadi, hech qanday xabar yubormaydi. */
    @PostMapping("/leads")
    public Mono<ApiResponse<LeadDto>> createLead(@Valid @RequestBody LeadRequest request) {
        return landingService.createLead(request)
                .map(lead -> ApiResponse.ok("Arizangiz qabul qilindi", lead));
    }
}
