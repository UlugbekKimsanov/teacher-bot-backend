package uz.sevenEdu.teacherBot.payment.payme.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.payme.dto.PaymeRequest;
import uz.sevenEdu.teacherBot.payment.payme.dto.PaymeResponse;
import uz.sevenEdu.teacherBot.payment.payme.service.PaymeService;

@RestController
@RequestMapping("/api/v1/payme")
@RequiredArgsConstructor
public class PaymeController {

    private final PaymeService paymeService;

    /**
     * Payme barcha metodlarini shu bitta POST endpointga yuboradi.
     * Authorization: Basic base64(Paycom:{secretKey})
     */
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<PaymeResponse> handle(
            @RequestBody PaymeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        return paymeService.handle(request, authHeader);
    }
}
