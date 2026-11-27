package uz.sevenEdu.teacherBot.lesson.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;

public interface LessonRepository extends ReactiveCrudRepository<Lesson, Long> {
    Flux<Lesson> findByCourseIdOrderByOrderIndex(Long courseId);

    @Query("SELECT COUNT(*) FROM lessons WHERE course_id = :courseId")
    Mono<Long> countByCourseId(Long courseId);

    @Query("SELECT COALESCE(SUM(duration_sec), 0) FROM lessons WHERE course_id = :courseId")
    Mono<Long> sumDurationByCourseId(Long courseId);

    /** Bir nechta kurs darslarini bitta so'rovda (N+1 o'rniga batch). */
    @Query("SELECT * FROM lessons WHERE course_id IN (:courseIds) ORDER BY course_id ASC, order_index ASC")
    Flux<Lesson> findByCourseIdInOrdered(java.util.Collection<Long> courseIds);

    @Query("SELECT * FROM lessons WHERE course_id = :courseId AND order_index > :orderIndex ORDER BY order_index ASC LIMIT 1")
    Mono<Lesson> findNextLessonInCourse(Long courseId, Integer orderIndex);

    /** Faylni (video, tejamkor video yoki muqova) nechta dars ishlatayotgani — o'chirishdan oldingi tekshiruv uchun. */
    @Query("SELECT COUNT(*) FROM lessons WHERE video_url = :path OR video_url_low = :path OR cover_image = :path")
    Mono<Long> countFileReferences(String path);

    // ── Tejamkor video varianti (VideoTranscodeService) ─────────────
    // Fon jobi butun entity'ni save qilmaydi (admin bir vaqtda tahrirlagan maydonlarni
    // bosib ketmaslik uchun) — faqat kerakli ustunlar nishonli UPDATE bilan yoziladi.

    /** Ishlanishi kerak bo'lgan darslar: lokal video + holat NULL/PENDING/PROCESSING (restartdan qolgan). */
    @Query("SELECT id FROM lessons WHERE video_url IS NOT NULL AND video_url <> '' "
            + "AND video_url NOT LIKE 'http://%' AND video_url NOT LIKE 'https://%' "
            + "AND (video_low_status IS NULL OR video_low_status IN ('PENDING', 'PROCESSING')) ORDER BY id")
    Flux<Long> findIdsNeedingVideoVariant();

    /** Ma'lum xato bilan FAILED bo'lgan lokal videolar (masalan "ffmpeg topilmadi" — o'rnatilgach qayta urinish). */
    @Query("SELECT id FROM lessons WHERE video_low_status = 'FAILED' AND video_low_error LIKE CONCAT(:errorPrefix, '%') "
            + "AND video_url IS NOT NULL AND video_url <> '' "
            + "AND video_url NOT LIKE 'http://%' AND video_url NOT LIKE 'https://%' ORDER BY id")
    Flux<Long> findIdsFailedWithError(String errorPrefix);

    @Modifying
    @Query("UPDATE lessons SET video_low_status = :status, video_low_error = :error WHERE id = :id")
    Mono<Integer> updateVideoLowStatus(Long id, String status, String error);

    /** Holatni faqat video o'zgarmagan bo'lsa yozadi (poyga himoyasi). Natija — yangilangan qatorlar soni. */
    @Modifying
    @Query("UPDATE lessons SET video_low_status = :status, video_low_error = :error "
            + "WHERE id = :id AND video_url = :videoUrl")
    Mono<Integer> updateVideoLowStatusIfVideo(Long id, String videoUrl, String status, String error);

    /** Faststart qilingan yangi faylga almashtirish — faqat video_url hali eski qiymatda bo'lsa. */
    @Modifying
    @Query("UPDATE lessons SET video_url = :newUrl WHERE id = :id AND video_url = :oldUrl")
    Mono<Integer> replaceVideoUrl(Long id, String oldUrl, String newUrl);

    /** Tejamkor variant natijasi — faqat manba video o'zgarmagan bo'lsa. */
    @Modifying
    @Query("UPDATE lessons SET video_url_low = :lowUrl, video_low_status = :status, video_low_error = NULL "
            + "WHERE id = :id AND video_url = :videoUrl")
    Mono<Integer> saveVideoLowResult(Long id, String videoUrl, String lowUrl, String status);
}
