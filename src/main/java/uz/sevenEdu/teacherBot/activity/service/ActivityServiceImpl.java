package uz.sevenEdu.teacherBot.activity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.activity.dto.ActivityRequest;
import uz.sevenEdu.teacherBot.activity.dto.ActivitySummaryDto;
import uz.sevenEdu.teacherBot.activity.entity.Activity;
import uz.sevenEdu.teacherBot.activity.enums.ActivityType;
import uz.sevenEdu.teacherBot.activity.enums.PeriodType;
import uz.sevenEdu.teacherBot.activity.repository.ActivityRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;

    @Override
    public Mono<Activity> create(ActivityRequest request) {
        Activity activity = Activity.builder()
                .activityType(request.getActivityType())
                .measureType(request.getMeasureType())
                .value(request.getValue())
                .studentId(request.getStudentId())
                .createdAt(LocalDateTime.now())
                .build();
        return activityRepository.save(activity);
    }

    @Override
    public Mono<List<ActivitySummaryDto>> getSummary(Long userId, PeriodType period) {
        LocalDateTime from = calculateFrom(period);

        return activityRepository.findSummaryByStudentIdAndPeriod(userId, from)
                .collectMap(
                        row -> ActivityType.valueOf(row.getActivityType()),
                        row -> row.getValue() != null ? row.getValue() : 0L
                )
                .map(dbMap -> buildFullSummary(dbMap));
    }

    private List<ActivitySummaryDto> buildFullSummary(Map<ActivityType, Long> dbMap) {
        return Arrays.stream(ActivityType.values())
                .map(type -> ActivitySummaryDto.builder()
                        .activityType(type)
                        .value(dbMap.getOrDefault(type, 0L))
                        .build())
                .collect(Collectors.toList());
    }

    private LocalDateTime calculateFrom(PeriodType period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case DAILY -> now.toLocalDate().atStartOfDay();
            case WEEKLY -> now.toLocalDate().minusDays(7).atStartOfDay();
            case MONTHLY -> now.toLocalDate().minusMonths(1).atStartOfDay();
        };
    }
}
