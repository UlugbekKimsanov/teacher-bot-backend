package uz.sevenEdu.teacherBot.activity.service;

import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.activity.dto.ActivityRequest;
import uz.sevenEdu.teacherBot.activity.dto.ActivitySummaryDto;
import uz.sevenEdu.teacherBot.activity.entity.Activity;
import uz.sevenEdu.teacherBot.activity.enums.PeriodType;

import java.util.List;

public interface ActivityService {
    Mono<Activity> create(ActivityRequest request);
    Mono<List<ActivitySummaryDto>> getSummary(Long userId, PeriodType period);
}
