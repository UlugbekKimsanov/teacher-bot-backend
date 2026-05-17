package uz.sevenEdu.teacherBot.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.rating.dto.RatingDto;
import uz.sevenEdu.teacherBot.rating.service.RatingService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rating")
@RequiredArgsConstructor
public class RatingController {
    private final RatingService ratingService;

    @GetMapping("/attendance/{courseId}")
    public Mono<ApiResponse<RatingDto.AttendanceDto>> getAttendance(@PathVariable Long courseId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ratingService.getAttendance(userId, courseId).map(ApiResponse::ok);
    }

    @GetMapping("/points")
    public Mono<ApiResponse<RatingDto.PointsSummary>> getPoints(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ratingService.getPoints(userId).map(ApiResponse::ok);
    }

    @GetMapping("/progress/{courseId}")
    public Mono<ApiResponse<RatingDto.ProgressDto>> getProgress(@PathVariable Long courseId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ratingService.getProgress(userId, courseId).map(ApiResponse::ok);
    }

    @GetMapping("/certificates")
    public Mono<ApiResponse<List<RatingDto.CertificateDto>>> getCertificates(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ratingService.getCertificates(userId).collectList().map(ApiResponse::ok);
    }

    @GetMapping("/streak")
    public Mono<ApiResponse<RatingDto.StreakDto>> getStreak(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ratingService.getStreak(userId).map(ApiResponse::ok);
    }

    @PostMapping("/streak/record")
    public Mono<ApiResponse<String>> recordAttendance(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ratingService.recordAttendance(userId).thenReturn(ApiResponse.ok("recorded"));
    }

    @GetMapping("/leaderboard")
    public Mono<ApiResponse<RatingDto.LeaderboardDto>> getLeaderboard(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ratingService.getLeaderboard(userId).map(ApiResponse::ok);
    }

    @GetMapping("/daily-goals")
    public Mono<ApiResponse<RatingDto.DailyGoalsDto>> getDailyGoals(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ratingService.getDailyGoals(userId).map(ApiResponse::ok);
    }
}
