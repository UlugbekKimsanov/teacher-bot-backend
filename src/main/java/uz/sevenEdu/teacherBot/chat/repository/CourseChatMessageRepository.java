package uz.sevenEdu.teacherBot.chat.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.chat.entity.CourseChatMessage;

import java.time.LocalDateTime;

public interface CourseChatMessageRepository extends ReactiveCrudRepository<CourseChatMessage, Long> {

    @Query("SELECT * FROM course_chat_messages WHERE course_id = :courseId ORDER BY created_at ASC")
    Flux<CourseChatMessage> findByCourseId(Long courseId);

    @Query("SELECT * FROM course_chat_messages WHERE media_path IS NOT NULL AND created_at < :before")
    Flux<CourseChatMessage> findExpiredMedia(LocalDateTime before);

    Mono<CourseChatMessage> findFirstByCourseIdAndConferenceActiveTrueAndMessageType(Long courseId, String messageType);

    @Modifying
    @Query("UPDATE course_chat_messages SET conference_active = false, pinned = false " +
            "WHERE course_id = :courseId AND message_type = 'conference' AND conference_active = true")
    Mono<Integer> deactivateActiveConferences(Long courseId);
}
