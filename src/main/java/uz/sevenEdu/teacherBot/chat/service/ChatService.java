package uz.sevenEdu.teacherBot.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.chat.dto.ChatMessageDto;
import uz.sevenEdu.teacherBot.chat.entity.ChatMessage;
import uz.sevenEdu.teacherBot.chat.entity.CourseChatMessage;
import uz.sevenEdu.teacherBot.chat.repository.ChatMessageRepository;
import uz.sevenEdu.teacherBot.chat.repository.CourseChatMessageRepository;
import uz.sevenEdu.teacherBot.chat.repository.CourseTeacherRepository;
import uz.sevenEdu.teacherBot.common.service.FileStorageService;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.notification.entity.Notification;
import uz.sevenEdu.teacherBot.notification.repository.NotificationRepository;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final CourseChatMessageRepository courseChatMessageRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final UserCourseRepository userCourseRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FileStorageService fileStorageService;
    private final uz.sevenEdu.teacherBot.course.repository.CourseRepository courseRepository;
    private final uz.sevenEdu.teacherBot.course.repository.LanguageRepository languageRepository;

    // ═══════════════════════════════════════════════════════
    // SHAXSIY CHAT (o'quvchi ↔ o'qituvchi)
    // ═══════════════════════════════════════════════════════

    public Flux<ChatMessageDto> getHistory(Long courseId, Long studentId) {
        return chatMessageRepository.findByCourseAndStudent(courseId, studentId)
                .map(this::toDto);
    }

    public Mono<ChatMessageDto> saveMessage(Long courseId, Long studentId, Long senderId,
                                            String senderRole, String text,
                                            String mediaPath, String mediaType) {
        ChatMessage msg = ChatMessage.builder()
                .courseId(courseId)
                .studentId(studentId)
                .senderId(senderId)
                .senderRole(senderRole)
                .text(text)
                .mediaPath(mediaPath)
                .mediaType(mediaType)
                .createdAt(LocalDateTime.now())
                .build();
        return chatMessageRepository.save(msg)
                .flatMap(saved -> {
                    ChatMessageDto dto = toDto(saved);
                    if ("teacher".equalsIgnoreCase(senderRole)) {
                        Notification n = Notification.builder()
                                .userId(studentId)
                                .title("Ustoz xabar yubordi")
                                .body(notifBody(text, mediaType))
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
        // 1) Kursga to'g'ridan-to'g'ri biriktirilgan bo'lsa
        return courseTeacherRepository.findByCourseIdAndTeacherId(courseId, teacherId)
                .map(ct -> true)
                .defaultIfEmpty(false)
                .flatMap(assigned -> assigned
                        ? Mono.just(true)
                        // 2) Yoki o'qituvchi mutaxassisligi (tillari) kurs tiliga mos kelsa
                        : specializationCoversCourse(courseId, teacherId));
    }

    /** O'qituvchi mutaxassisligidagi tillar kursning tiliga mos keladimi. */
    private Mono<Boolean> specializationCoversCourse(Long courseId, Long teacherId) {
        return courseRepository.findById(courseId)
                .flatMap(course -> {
                    if (course.getLanguageId() == null) return Mono.just(false);
                    return Mono.zip(
                            languageRepository.findById(course.getLanguageId()),
                            userRepository.findById(teacherId)
                    ).map(t -> {
                        String langName = t.getT1().getName();
                        String spec = t.getT2().getSpecialization();
                        if (langName == null || spec == null || spec.isBlank()) return false;
                        for (String s : spec.split(",")) {
                            if (s.trim().equalsIgnoreCase(langName.trim())) return true;
                        }
                        return false;
                    });
                })
                .defaultIfEmpty(false);
    }

    // ═══════════════════════════════════════════════════════
    // KURS GURUH CHAT (barcha o'quvchi + o'qituvchilar)
    // ═══════════════════════════════════════════════════════

    public Flux<ChatMessageDto> getGroupHistory(Long courseId) {
        return courseChatMessageRepository.findByCourseId(courseId)
                .map(this::toGroupDto);
    }

    public Mono<ChatMessageDto> saveGroupMessage(Long courseId, Long senderId, String senderRole,
                                                 String senderName, String text,
                                                 String mediaPath, String mediaType) {
        CourseChatMessage msg = CourseChatMessage.builder()
                .courseId(courseId)
                .senderId(senderId)
                .senderRole(senderRole)
                .senderName(senderName)
                .text(text)
                .mediaPath(mediaPath)
                .mediaType(mediaType)
                .createdAt(LocalDateTime.now())
                .build();
        return courseChatMessageRepository.save(msg).map(this::toGroupDto);
    }

    /**
     * Foydalanuvchi kurs a'zosimi: ro'yxatdan o'tgan o'quvchi YOKI shu kursning o'qituvchisi.
     */
    public Mono<Boolean> isMemberOfCourse(Long courseId, Long userId) {
        return userCourseRepository.existsByUserIdAndCourseId(userId, courseId)
                .flatMap(enrolled -> enrolled
                        ? Mono.just(true)
                        : isTeacherOfCourse(courseId, userId));
    }

    /**
     * Foydalanuvchining to'liq ismini qaytaradi (xabar ustida ko'rsatish uchun).
     */
    public Mono<String> userFullName(Long userId) {
        return userRepository.findById(userId)
                .map(u -> {
                    String fn = u.getFirstName() != null ? u.getFirstName() : "";
                    String ln = u.getLastName() != null ? u.getLastName() : "";
                    String full = (fn + " " + ln).trim();
                    return full.isEmpty() ? "Foydalanuvchi" : full;
                })
                .defaultIfEmpty("Foydalanuvchi");
    }

    // ═══════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════

    private String notifBody(String text, String mediaType) {
        if (text != null && !text.isBlank()) {
            return text.length() > 100 ? text.substring(0, 100) + "..." : text;
        }
        if ("image".equals(mediaType)) return "📷 Rasm";
        if (mediaType != null) return "📎 Fayl";
        return "";
    }

    private ChatMessageDto toDto(ChatMessage m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .courseId(m.getCourseId())
                .studentId(m.getStudentId())
                .senderId(m.getSenderId())
                .senderRole(m.getSenderRole())
                .text(m.getText())
                .mediaUrl(fileStorageService.toPublicUrl(m.getMediaPath()))
                .mediaType(m.getMediaType())
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                .type("message")
                .build();
    }

    private ChatMessageDto toGroupDto(CourseChatMessage m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .courseId(m.getCourseId())
                .senderId(m.getSenderId())
                .senderRole(m.getSenderRole())
                .senderName(m.getSenderName())
                .text(m.getText())
                .mediaUrl(fileStorageService.toPublicUrl(m.getMediaPath()))
                .mediaType(m.getMediaType())
                .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                .type("message")
                .build();
    }
}
