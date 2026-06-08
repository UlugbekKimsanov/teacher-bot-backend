package uz.sevenEdu.teacherBot.payment.click.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.click.dto.ClickCompleteRequestDto;
import uz.sevenEdu.teacherBot.payment.click.dto.ClickPrepareRequestDto;
import uz.sevenEdu.teacherBot.payment.click.dto.ClickResponseDto;
import uz.sevenEdu.teacherBot.payment.click.service.ClickService;

@Tag(name = "Click Payment", description = "Click to'lov tizimi integratsiyasi")
@RestController
@RequestMapping("/api/v1/click")
@RequiredArgsConstructor
public class ClickController {

    private final ClickService clickService;

    @Operation(summary = "Prepare — to'lovni tayyorlash (action=0)")
    @PostMapping("/prepare")
    public Mono<ClickResponseDto> prepare(@RequestBody ClickPrepareRequestDto request) {
        return clickService.prepare(request);
    }

    @Operation(summary = "Complete — to'lovni yakunlash (action=1)")
    @PostMapping("/complete")
    public Mono<ClickResponseDto> complete(@RequestBody ClickCompleteRequestDto request) {
        return clickService.complete(request);
    }
}
