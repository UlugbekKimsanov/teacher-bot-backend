package uz.sevenEdu.teacherBot.chat.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.chat.entity.ChatMessage;

public interface ChatMessageRepository extends ReactiveCrudRepository<ChatMessage, Long> {

    @Query("SELECT * FROM chat_messages WHERE course_id = :courseId AND student_id = :studentId ORDER BY created_at ASC")
    Flux<ChatMessage> findByCourseAndStudent(Long courseId, Long studentId);
}
