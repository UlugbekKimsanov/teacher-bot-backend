package uz.sevenEdu.teacherBot.rating.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.dto.RatingDto;

public interface RatingService {
    Mono<RatingDto.AttendanceDto> getAttendance(Long userId, Long courseId);
    Mono<RatingDto.PointsSummary> getPoints(Long userId);
    Mono<RatingDto.ProgressDto> getProgress(Long userId, Long courseId);
    Flux<RatingDto.CertificateDto> getCertificates(Long userId);
}
