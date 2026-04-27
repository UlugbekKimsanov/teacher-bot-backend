package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.sevenEdu.teacherBot.rating.entity.Certificate;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByUserId(Long userId);
    Optional<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);
}
