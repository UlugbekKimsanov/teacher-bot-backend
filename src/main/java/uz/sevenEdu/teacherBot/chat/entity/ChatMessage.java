package uz.sevenEdu.teacherBot.chat.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_messages")
public class ChatMessage {
    @Id
    private Long id;
    private Long courseId;
    private Long studentId;
    private Long senderId;
    private String senderRole; // "user" or "teacher"
    private String text;
    private String mediaPath;  // yuklangan media yo'li
    private String mediaType;  // "image" or "file"
    private LocalDateTime createdAt;
}
