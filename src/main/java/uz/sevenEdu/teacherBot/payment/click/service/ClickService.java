package uz.sevenEdu.teacherBot.payment.click.service;

import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.payment.click.dto.ClickCompleteRequestDto;
import uz.sevenEdu.teacherBot.payment.click.dto.ClickPrepareRequestDto;
import uz.sevenEdu.teacherBot.payment.click.dto.ClickResponseDto;

public interface ClickService {
    Mono<ClickResponseDto> prepare(ClickPrepareRequestDto request);
    Mono<ClickResponseDto> complete(ClickCompleteRequestDto request);
}
