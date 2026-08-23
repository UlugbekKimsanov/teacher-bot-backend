package uz.sevenEdu.teacherBot.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Leaderboard uchun agregat so'rov natijasi (bitta GROUP BY so'rovdan). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardRow {
    private Long id;
    private String firstName;
    private String lastName;
    private Long total;
}
