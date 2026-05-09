package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class LessonDetailDto {
    private Long id;
    private Long courseId;
    private String title;
    private String videoUrl;
    private Integer orderIndex;
    private Integer durationSec;
    private boolean completed;
    private boolean locked;
    private Integer vocabScore;
    private Integer testScore;
    private Integer exerciseScore;
    private List<VocabDto> vocabulary;
    private List<QuestionDto> questions;
    private List<ExerciseDto> exercises;

    @Data @Builder
    public static class VocabDto {
        private Long id;
        private String phraseUz;
        private String phraseEn;
    }

    @Data @Builder
    public static class QuestionDto {
        private Long id;
        private String questionText;
        private String optionA;
        private String optionB;
        private String optionC;
    }

    @Data @Builder
    public static class ExerciseDto {
        private Long id;
        private String sentence;
        private String options;
        private String correctAnswer;
        private Integer orderIndex;
    }
}
