package uz.sevenEdu.teacherBot.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** Admin yuborgan bildirishnoma — har yuborish uchun bitta qator (tarix sahifasi uchun). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("admin_notification_log")
public class AdminNotificationLog {
    @Id
    private Long id;
    private String title;
    private String body;
    private String image;
    private String target;          // ALL | STUDENTS | TEACHERS | COURSE | USERS; eski yozuvlarda null
    private Long courseId;
    private Integer recipientCount;
    private Long sentBy;
    private LocalDateTime createdAt;
}
