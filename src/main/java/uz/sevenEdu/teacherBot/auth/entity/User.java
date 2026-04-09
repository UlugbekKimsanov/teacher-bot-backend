package uz.sevenEdu.teacherBot.auth.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    @Id
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String password;
    private String role;
    private LocalDateTime createdAt;
}
