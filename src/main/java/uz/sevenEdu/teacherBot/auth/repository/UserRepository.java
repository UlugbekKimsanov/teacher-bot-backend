package uz.sevenEdu.teacherBot.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.sevenEdu.teacherBot.auth.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
