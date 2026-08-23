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
    private String senderName;
    private String text;
    private String mediaUrl;   // public URL (/files/...)
    private String mediaType;  // "image" or "file"
    private String createdAt;
    private String type; // "message", "history", "error"
    private String messageType; // "message" or "conference"
    private Boolean pinned;
    private String conferenceUrl;
    private Boolean conferenceActive;
    private String conferenceStartAt;
}
