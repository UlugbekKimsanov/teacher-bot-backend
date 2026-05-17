package uz.sevenEdu.teacherBot.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private Long id;
    private Long courseId;
    private Long studentId;
    private Long senderId;
    private String senderRole;
    private String text;
    private String createdAt;
    private String type; // "message", "history", "error"
}
