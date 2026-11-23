package uz.sevenEdu.teacherBot.chat.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public interface ChatMessageRepository extends ReactiveCrudRepository<ChatMessage, Long> {

    @Query("SELECT * FROM chat_messages WHERE course_id = :courseId AND student_id = :studentId ORDER BY created_at ASC")
    Flux<ChatMessage> findByCourseAndStudent(Long courseId, Long studentId);

    /** Har bir (kurs, o'quvchi) xonasining eng oxirgi xabari — ustoz paneli ro'yxati uchun. */
    @Query("SELECT DISTINCT ON (course_id, student_id) * FROM chat_messages " +
            "WHERE course_id IN (:courseIds) " +
            "ORDER BY course_id, student_id, created_at DESC, id DESC")
    Flux<ChatMessage> findLastPerRoom(java.util.Collection<Long> courseIds);

    @Query("SELECT * FROM chat_messages WHERE media_path IS NOT NULL AND created_at < :before")
    Flux<ChatMessage> findExpiredMedia(LocalDateTime before);
}
