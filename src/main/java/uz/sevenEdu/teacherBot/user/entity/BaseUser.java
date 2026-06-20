package uz.sevenEdu.teacherBot.user.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uz.sevenEdu.teacherBot.user.enums.UserRole;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class BaseUser {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private String address;
    private String avatarUrl;
    private UserRole role;
    private Long ball;
    private String specialization;
    private String fcmToken;
    private Long telegramChatId;
    private Boolean isDefault;
    private Boolean isGuest;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
}
