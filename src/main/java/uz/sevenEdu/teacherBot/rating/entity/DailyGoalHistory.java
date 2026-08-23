package uz.sevenEdu.teacherBot.rating.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/** Kunlik maqsad tarixi — kun yakunida (23:59) cron yozadi. Foiz o'sha kungi
 *  maqsadga nisbatan hisoblanib saqlanadi (kelajakda o'rtacha foizni to'g'ri
 *  hisoblash uchun). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("daily_goal_history")
public class DailyGoalHistory {
    @Id
    private Long id;
    private Long userId;
    private LocalDate historyDate;
    private Integer minutesDone;
    private Integer wordsDone;
    private Integer minutesGoal;
    private Integer wordsGoal;
    private Double percent;
}
