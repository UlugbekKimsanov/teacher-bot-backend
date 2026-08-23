package uz.sevenEdu.teacherBot.rating.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

/** Kunlik faollik — ilovada o'tkazilgan vaqt (sekundlarda).
 *  PK (user_id, activity_date) — faqat @Query orqali ishlatiladi. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("daily_activity")
public class DailyActivity {
    @Id
    private Long userId;
    private LocalDate activityDate;
    private Integer secondsSpent;
}
