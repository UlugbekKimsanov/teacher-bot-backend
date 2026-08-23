package uz.sevenEdu.teacherBot.rating.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.sevenEdu.teacherBot.rating.service.RatingService;

/** Har kuni 23:59 da barcha real o'quvchilarning bugungi maqsad natijasi va
 *  foizini (o'sha kungi maqsadga nisbatan) daily_goal_history'ga saqlaydi.
 *  Shu sabab o'rtacha foiz keyinchalik eski maqsadlar bo'yicha to'g'ri hisoblanadi. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyGoalScheduler {

    private final RatingService ratingService;

    @Scheduled(cron = "0 59 23 * * *")
    public void snapshotDailyGoals() {
        ratingService.snapshotAllDailyGoals()
                .doOnError(e -> log.warn("Daily goal snapshot xatosi: {}", e.getMessage()))
                .subscribe();
    }
}
