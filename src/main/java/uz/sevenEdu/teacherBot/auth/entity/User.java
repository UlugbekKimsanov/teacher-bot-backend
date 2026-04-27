package uz.sevenEdu.teacherBot.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import uz.sevenEdu.teacherBot.auth.enums.UserRole;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private String address;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    private LocalDateTime createdAt;
}
