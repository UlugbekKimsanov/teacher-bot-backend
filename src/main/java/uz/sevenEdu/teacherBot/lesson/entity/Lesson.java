package uz.sevenEdu.teacherBot.lesson.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("lessons")
public class Lesson {
    @Id
    private Long id;
    private Long courseId;
    private String name;
    private String description;
    private Integer orderIndex;
    private Integer durationSec;
    private String coverImage;
    private String videoUrl;
    /** YouTube unlisted video ID — pullik dars videosi YouTube orqali beriladi. */
    private String youtubeId;
    /**
     * Tejamkor (~480p) video varianti — nisbiy yo'l. Server fonda ffmpeg bilan yaratadi
     * (VideoTranscodeService). Tayyor bo'lmasa null.
     */
    private String videoUrlLow;
    /** Tejamkor variant holati: null (hali ishlanmagan), PENDING, PROCESSING, READY, SKIPPED, FAILED. */
    private String videoLowStatus;
    /** FAILED bo'lsa — qisqa xato matni. */
    private String videoLowError;

    // Tejamkor variant holatlari (video_low_status ustuni qiymatlari)
    public static final String LOW_PENDING = "PENDING";
    public static final String LOW_PROCESSING = "PROCESSING";
    public static final String LOW_READY = "READY";
    public static final String LOW_SKIPPED = "SKIPPED";
    public static final String LOW_FAILED = "FAILED";

    /** Video storage'dagi lokal fayl bo'lsa (tashqi http/https URL emas) — true. */
    public static boolean isLocalVideo(String videoUrl) {
        return videoUrl != null && !videoUrl.isBlank()
                && !videoUrl.startsWith("http://") && !videoUrl.startsWith("https://");
    }
}
