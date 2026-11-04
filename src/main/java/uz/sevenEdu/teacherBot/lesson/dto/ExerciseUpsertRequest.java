package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Data;

import java.util.List;

/** Admin: mashq yaratish/tahrirlash so'rovi. */
@Data
public class ExerciseUpsertRequest {
    /** Gap; har tushib qolgan so'z o'rnida '_' (bir nechta bo'lishi mumkin). */
    private String sentence;
    /** Chalg'ituvchi so'zlar, vergul bilan. */
    private String options;
    /** Gapdagi '_' guruhlari tartibida to'g'ri javoblar. */
    private List<String> correctAnswers;
    private String name;
    private Integer orderIndex;
}
