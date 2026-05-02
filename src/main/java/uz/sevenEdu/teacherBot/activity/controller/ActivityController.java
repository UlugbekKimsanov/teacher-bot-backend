package uz.sevenEdu.teacherBot.activity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.activity.dto.ActivityRequest;
import uz.sevenEdu.teacherBot.activity.dto.ActivitySummaryDto;
import uz.sevenEdu.teacherBot.activity.entity.Activity;
import uz.sevenEdu.teacherBot.activity.enums.PeriodType;
import uz.sevenEdu.teacherBot.activity.service.ActivityService;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public Mono<ApiResponse<Activity>> create(@Valid @RequestBody ActivityRequest request) {
        return activityService.create(request).map(ApiResponse::ok);
    }

    @GetMapping("/{userId}")
    public Mono<ApiResponse<List<ActivitySummaryDto>>> getSummary(
            @PathVariable Long userId,
            @RequestParam PeriodType period) {
        return activityService.getSummary(userId, period).map(ApiResponse::ok);
    }
}
