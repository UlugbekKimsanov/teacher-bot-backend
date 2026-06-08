package uz.sevenEdu.teacherBot.payment.paynet.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.paynet.dto.PaynetRequest;
import uz.sevenEdu.teacherBot.payment.paynet.dto.PaynetResponse;
import uz.sevenEdu.teacherBot.payment.paynet.service.PaynetService;

@RestController
@RequestMapping("/api/v1/paynet")
@RequiredArgsConstructor
public class PaynetController {

    private final PaynetService paynetService;

    /**
     * Paynet barcha metodlarini shu bitta POST endpointga yuboradi.
     * Authorization: Basic base64(username:password)
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<PaynetResponse> handle(
            @RequestBody PaynetRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return paynetService.handle(request, authHeader);
    }
}
