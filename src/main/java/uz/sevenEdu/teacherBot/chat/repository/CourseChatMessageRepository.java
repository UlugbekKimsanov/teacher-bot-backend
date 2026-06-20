package uz.sevenEdu.teacherBot.chat.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.chat.entity.CourseChatMessage;

import java.time.LocalDateTime;

public interface CourseChatMessageRepository extends ReactiveCrudRepository<CourseChatMessage, Long> {

    @Query("SELECT * FROM course_chat_messages WHERE course_id = :courseId ORDER BY created_at ASC")
    Flux<CourseChatMessage> findByCourseId(Long courseId);

    @Query("SELECT * FROM course_chat_messages WHERE media_path IS NOT NULL AND created_at < :before")
    Flux<CourseChatMessage> findExpiredMedia(LocalDateTime before);
}
