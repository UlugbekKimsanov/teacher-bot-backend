package uz.sevenEdu.teacherBot.rating.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.rating.dto.RatingDto;

public interface RatingService {
    Mono<RatingDto.AttendanceDto> getAttendance(Long userId, Long courseId);
    Mono<RatingDto.PointsSummary> getPoints(Long userId);
    Mono<RatingDto.ProgressDto> getProgress(Long userId, Long courseId);
    Flux<RatingDto.CertificateDto> getCertificates(Long userId);
    Mono<RatingDto.StreakDto> getStreak(Long userId);
    Mono<Void> recordAttendance(Long userId);
    Mono<RatingDto.LeaderboardDto> getLeaderboard(Long userId);
    Mono<RatingDto.DailyGoalsDto> getDailyGoals(Long userId);

    /** Ilovada o'tkazilgan vaqtni (sekund) bugungi faollikka qo'shadi. */
    Mono<Void> recordActivity(Long userId, int seconds);

    /** Foydalanuvchi kunlik maqsadini (daqiqa/so'z) o'zgartiradi. */
    Mono<RatingDto.DailyGoalsDto> updateGoals(Long userId, int minutesGoal, int wordsGoal);

    /** Maqsadlar tarixi + o'rtacha foiz + bugungi maqsad. */
    Mono<RatingDto.GoalsHistoryDto> getGoalsHistory(Long userId);

    /** Kun yakunida (cron) barcha real o'quvchilar maqsadini tarixga saqlaydi. */
    Mono<Void> snapshotAllDailyGoals();
}
