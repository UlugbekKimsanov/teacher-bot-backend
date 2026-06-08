package uz.sevenEdu.teacherBot.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.rating.dto.RatingDto;
import uz.sevenEdu.teacherBot.rating.service.AchievementService;
import uz.sevenEdu.teacherBot.rating.service.RatingService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rating")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;
    private final AchievementService achievementService;

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }

    @GetMapping("/attendance/{courseId}")
    public Mono<ApiResponse<RatingDto.AttendanceDto>> getAttendance(@PathVariable Long courseId, Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return Mono.just(ApiResponse.ok(RatingDto.AttendanceDto.builder()
                .courseName("").weeklyMissed(0).monthlyMissed(0).quarterlyMissed(0).build()));
        return ratingService.getAttendance(userId, courseId).map(ApiResponse::ok);
    }

    @GetMapping("/points")
    public Mono<ApiResponse<RatingDto.PointsSummary>> getPoints(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return Mono.just(ApiResponse.ok(RatingDto.PointsSummary.builder()
                .entries(List.of()).total(0L).build()));
        return ratingService.getPoints(userId).map(ApiResponse::ok);
    }

    @GetMapping("/progress/{courseId}")
    public Mono<ApiResponse<RatingDto.ProgressDto>> getProgress(@PathVariable Long courseId, Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return Mono.just(ApiResponse.ok(RatingDto.ProgressDto.builder()
                .courseName("").vocabularyScore(0).testScore(0).questionsScore(0).maxScore(10).build()));
        return ratingService.getProgress(userId, courseId).map(ApiResponse::ok);
    }

    @GetMapping("/certificates")
    public Mono<ApiResponse<List<RatingDto.CertificateDto>>> getCertificates(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return Mono.just(ApiResponse.ok(List.of()));
        return ratingService.getCertificates(userId).collectList().map(ApiResponse::ok);
    }

    @GetMapping("/streak")
    public Mono<ApiResponse<RatingDto.StreakDto>> getStreak(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return Mono.just(ApiResponse.ok(RatingDto.StreakDto.builder()
                .currentStreak(0).longestStreak(0).studiedDays(List.of()).weekDays(List.of(false,false,false,false,false,false,false)).build()));
        return ratingService.getStreak(userId).map(ApiResponse::ok);
    }

    @PostMapping("/streak/record")
    public Mono<ApiResponse<String>> recordAttendance(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return Mono.just(ApiResponse.ok("skipped"));
        return ratingService.recordAttendance(userId).thenReturn(ApiResponse.ok("recorded"));
    }

    @GetMapping("/leaderboard")
    public Mono<ApiResponse<RatingDto.LeaderboardDto>> getLeaderboard(Authentication auth) {
        Long userId = getUserId(auth);
        return ratingService.getLeaderboard(userId != null ? userId : 0L).map(ApiResponse::ok);
    }

    @GetMapping("/achievements")
    public Mono<ApiResponse<List<AchievementService.AchievementDto>>> getAchievements(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return Mono.just(ApiResponse.ok(List.of()));
        // Avval yangi achievementlarni tekshir, keyin hammasini qaytar
        return achievementService.checkAndUnlock(userId)
                .then(achievementService.getAllForUser(userId))
                .map(ApiResponse::ok);
    }

    @GetMapping("/daily-goals")
    public Mono<ApiResponse<RatingDto.DailyGoalsDto>> getDailyGoals(Authentication auth) {
        Long userId = getUserId(auth);
        if (userId == null) return Mono.just(ApiResponse.ok(RatingDto.DailyGoalsDto.builder()
                .lessonsGoal(3).lessonsDone(0).minutesGoal(30).minutesDone(0).wordsGoal(20).wordsDone(0).build()));
        return ratingService.getDailyGoals(userId).map(ApiResponse::ok);
    }
}
