package uz.sevenEdu.teacherBot.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.chat.dto.ChatMessageDto;
import uz.sevenEdu.teacherBot.chat.entity.ChatMessage;
import uz.sevenEdu.teacherBot.chat.repository.ChatMessageRepository;
import uz.sevenEdu.teacherBot.chat.repository.CourseTeacherRepository;
import uz.sevenEdu.teacherBot.notification.entity.Notification;
import uz.sevenEdu.teacherBot.notification.repository.NotificationRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final NotificationRepository notificationRepository;

    public Flux<ChatMessageDto> getHistory(Long courseId, Long studentId) {
        return chatMessageRepository.findByCourseAndStudent(courseId, studentId)
                .map(this::toDto);
    }

    public Mono<ChatMessageDto> saveMessage(Long courseId, Long studentId, Long senderId, String senderRole, String text) {
        ChatMessage msg = ChatMessage.builder()
                .courseId(courseId)
                .studentId(studentId)
                .senderId(senderId)
                .senderRole(senderRole)
                .text(text)
                .createdAt(LocalDateTime.now())
                .build();
        return chatMessageRepository.save(msg)
                .flatMap(saved -> {
                    ChatMessageDto dto = toDto(saved);
                    if ("teacher".equalsIgnoreCase(senderRole)) {
                        Notification n = Notification.builder()
                                .userId(studentId)
                                .title("Ustoz xabar yubordi")
                                .body(text.length() > 100 ? text.substring(0, 100) + "..." : text)
                                .type("CHAT")
                                .refId(saved.getCourseId())
                                .isRead(false)
                                .createdAt(LocalDateTime.now())
                                .build();
                        return notificationRepository.save(n).thenReturn(dto);
                    }
                    return Mono.just(dto);
                });
    }

    public Mono<Boolean> isTeacherOfCourse(Long courseId, Long teacherId) {
        return courseTeacherRepository.findByCourseIdAndTeacherId(courseId, teacherId)
                .map(ct -> true)
                .defaultIfEmpty(false);
    }

    private ChatMessageDto toDto(ChatMessage m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .courseId(m.getCourseId())
                .studentId(m.getStudentId())
                .senderId(m.getSenderId())
                .senderRole(m.getSenderRole())
                .text(m.getText())
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                .type("message")
                .build();
    }
}
