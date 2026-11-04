package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SubmitResultDto {
    private int score;          // percentage 0-100
    private int points;         // score / 10
    private int stars;          // 0: <50%, 1: 50-70%, 2: 70-90%, 3: 90%+
    private boolean passed;     // score >= 50
    private List<UnlockedAchievement> achievements;
    /** Har bir savol/mashq bo'yicha natija — xatolarni qizil/yashil ko'rsatish uchun. */
    private List<ItemResult> items;

    @Data
    @Builder
    public static class ItemResult {
        /** test: savol id; mashq: bo'shliq (blank) id. */
        private Long id;
        /** Faqat mashq: bo'shliq qaysi mashqqa tegishli va gapdagi tartibi. */
        private Long exerciseId;
        private Integer position;
        private boolean correct;
        private String correctAnswer; // test: 'A'..'D' harfi; mashq: to'g'ri javob matni
    }

    @Data
    @Builder
    public static class UnlockedAchievement {
        private Long id;
        private String code;
        private String title;
        private String description;
        private String icon;
        private int bonusPoints;
    }

    public static SubmitResultDto from(int percentScore, List<UnlockedAchievement> achievements) {
        return from(percentScore, achievements, List.of());
    }

    public static SubmitResultDto from(int percentScore, List<UnlockedAchievement> achievements,
                                       List<ItemResult> items) {
        int stars;
        if (percentScore >= 90) stars = 3;
        else if (percentScore >= 70) stars = 2;
        else if (percentScore >= 50) stars = 1;
        else stars = 0;

        return SubmitResultDto.builder()
                .score(percentScore)
                .points(percentScore / 10)
                .stars(stars)
                .passed(percentScore >= 50)
                .achievements(achievements != null ? achievements : List.of())
                .items(items != null ? items : List.of())
                .build();
    }
}
