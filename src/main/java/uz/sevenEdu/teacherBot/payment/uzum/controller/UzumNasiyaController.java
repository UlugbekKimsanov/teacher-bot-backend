package uz.sevenEdu.teacherBot.payment.uzum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.uzum.dto.UzumNasiyaRequest;
import uz.sevenEdu.teacherBot.payment.uzum.dto.UzumNasiyaResponse;
import uz.sevenEdu.teacherBot.payment.uzum.service.UzumNasiyaService;

@RestController
@RequestMapping("/api/v1/uzum-nasiya")
@RequiredArgsConstructor
public class UzumNasiyaController {

    private final UzumNasiyaService uzumNasiyaService;

    @PostMapping(value = "/check", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UzumNasiyaResponse> check(
            @RequestBody UzumNasiyaRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!uzumNasiyaService.isAuthorized(authHeader)) {
            return Mono.just(UzumNasiyaResponse.error(401, "Ruxsat yo'q"));
        }
        return uzumNasiyaService.check(request);
    }

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UzumNasiyaResponse> create(
            @RequestBody UzumNasiyaRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!uzumNasiyaService.isAuthorized(authHeader)) {
            return Mono.just(UzumNasiyaResponse.error(401, "Ruxsat yo'q"));
        }
        return uzumNasiyaService.create(request);
    }

    @PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UzumNasiyaResponse> confirm(
            @RequestBody UzumNasiyaRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!uzumNasiyaService.isAuthorized(authHeader)) {
            return Mono.just(UzumNasiyaResponse.error(401, "Ruxsat yo'q"));
        }
        return uzumNasiyaService.confirm(request);
    }

    @PostMapping(value = "/reverse", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UzumNasiyaResponse> reverse(
            @RequestBody UzumNasiyaRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!uzumNasiyaService.isAuthorized(authHeader)) {
            return Mono.just(UzumNasiyaResponse.error(401, "Ruxsat yo'q"));
        }
        return uzumNasiyaService.reverse(request);
    }

    @PostMapping(value = "/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<UzumNasiyaResponse> status(
            @RequestBody UzumNasiyaRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!uzumNasiyaService.isAuthorized(authHeader)) {
            return Mono.just(UzumNasiyaResponse.error(401, "Ruxsat yo'q"));
        }
        return uzumNasiyaService.status(request);
    }
}
