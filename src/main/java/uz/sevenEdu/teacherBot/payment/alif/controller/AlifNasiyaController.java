package uz.sevenEdu.teacherBot.payment.alif.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.alif.dto.AlifAccountVerifyRequest;
import uz.sevenEdu.teacherBot.payment.alif.dto.AlifAccountVerifyResponse;
import uz.sevenEdu.teacherBot.payment.alif.dto.AlifWebhookRequest;
import uz.sevenEdu.teacherBot.payment.alif.service.AlifNasiyaService;

@RestController
@RequestMapping("/api/v1/alif-nasiya")
@RequiredArgsConstructor
public class AlifNasiyaController {

    private final AlifNasiyaService alifNasiyaService;

    /**
     * bePaid chaqiradi — mijoz akkauntini tekshirish.
     * Basic auth: shopId:secretKey
     */
    @PostMapping(
        value = "/account-verification",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<AlifAccountVerifyResponse> verifyAccount(
            @RequestBody AlifAccountVerifyRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return alifNasiyaService.verifyAccount(request, authHeader);
    }

    /**
     * bePaid webhook — tranzaksiya natijasi (successful/failed/expired).
     * shopId va secretKey body ichida keladi.
     */
    @PostMapping(
        value = "/webhook",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<Void> webhook(@RequestBody AlifWebhookRequest request) {
        return alifNasiyaService.handleWebhook(request);
    }
}
