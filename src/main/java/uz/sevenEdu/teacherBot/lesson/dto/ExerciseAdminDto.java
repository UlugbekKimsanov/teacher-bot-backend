package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Admin panel uchun mashq: gap + chalg'ituvchilar + har bo'shliqning to'g'ri javobi. */
@Data
@Builder
public class ExerciseAdminDto {
    private Long id;
    private Long lessonId;
    private String name;
    private Integer orderIndex;
    private String sentence;
    /** Faqat chalg'ituvchi so'zlar, vergul bilan (to'g'ri javoblar bu yerda emas). */
    private String options;
    /** position bo'yicha tartiblangan bo'shliqlar. */
    private List<ExerciseBlankDto> blanks;
}
