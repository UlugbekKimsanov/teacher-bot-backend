package uz.sevenEdu.teacherBot.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<ApiResponse<RatingDto.AttendanceDto>> getAttendance(
            @PathVariable Long courseId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(ratingService.getAttendance(userId, courseId)));
    }

    @GetMapping("/points")
    public ResponseEntity<ApiResponse<RatingDto.PointsSummary>> getPoints(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(ratingService.getPoints(userId)));
    }

    @GetMapping("/progress/{courseId}")
    public ResponseEntity<ApiResponse<RatingDto.ProgressDto>> getProgress(
            @PathVariable Long courseId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(ratingService.getProgress(userId, courseId)));
    }

    @GetMapping("/certificates")
    public ResponseEntity<ApiResponse<List<RatingDto.CertificateDto>>> getCertificates(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(ratingService.getCertificates(userId)));
    }
}
