package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class LessonDetailDto {
    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private String coverImage;
    private String videoUrl;
    /**
     * Tejamkor (~480p) video varianti — public /files/... URL. Faqat tayyor (READY) bo'lsa
     * va fayl diskda mavjud bo'lsa, aks holda null (mijoz videoUrl'dan foydalanadi).
     */
    private String videoUrlLow;
    /** Asl video hajmi (bayt) — lokal fayl bo'lsa; tashqi URL yoki fayl yo'q bo'lsa null. */
    private Long videoSize;
    /** Tejamkor variant hajmi (bayt) yoki null. */
    private Long videoLowSize;
    /**
     * YouTube unlisted video ID. Faqat haqli foydalanuvchiga qaytariladi:
     * kursga yozilganlarga — har doim; qolganlarga — faqat birinchi dars uchun, aks holda null.
     */
    private String youtubeId;
    private Integer orderIndex;
    private Integer durationSec;
    private boolean completed;
    private boolean locked;
    private Integer vocabScore;
    private Integer testScore;
    private Integer exerciseScore;
    private Long nextLessonId;
    private String teacherName; // kurs ustozi (video muallifi sifatida ko'rsatiladi)
    private List<VocabDto> vocabulary;
    private List<QuestionDto> questions;
    private List<ExerciseDto> exercises;
    private List<AudiobookDto> audiobooks;

    @Data @Builder
    public static class AudiobookDto {
        private Long id;
        private String title;
        private String description;
        private String url;     // audio
        private String pdfUrl;  // PDF (ixtiyoriy)
    }

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
        private String optionD;
    }

    @Data @Builder
    public static class ExerciseDto {
        private Long id;
        private String sentence;
        /** Barcha variantlar (to'g'ri javoblar + chalg'ituvchilar), vergul bilan — so'zlar banki uchun. */
        private String options;
        /** Eski mijozlar uchun: birinchi bo'shliqning to'g'ri javobi. */
        private String correctAnswer;
        private Integer orderIndex;
        /** Gapdagi '_' tartibida bo'shliqlar va ularning to'g'ri javoblari. */
        private List<ExerciseBlankDto> blanks;
    }
}
