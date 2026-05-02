package uz.sevenEdu.teacherBot.user.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import uz.sevenEdu.teacherBot.user.enums.UserRole;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseUser {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String password;
    private UserRole role;
    private LocalDateTime createdAt;
}
