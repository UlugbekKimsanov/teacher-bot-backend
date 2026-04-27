package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.sevenEdu.teacherBot.rating.entity.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query(value = "SELECT COUNT(*) FROM attendance WHERE user_id = :userId AND course_id = :courseId AND attended_at >= CURRENT_DATE - INTERVAL '7 days'", nativeQuery = true)
    long countWeekly(Long userId, Long courseId);

    @Query(value = "SELECT COUNT(*) FROM attendance WHERE user_id = :userId AND course_id = :courseId AND attended_at >= CURRENT_DATE - INTERVAL '30 days'", nativeQuery = true)
    long countMonthly(Long userId, Long courseId);

    @Query(value = "SELECT COUNT(*) FROM attendance WHERE user_id = :userId AND course_id = :courseId AND attended_at >= CURRENT_DATE - INTERVAL '120 days'", nativeQuery = true)
    long countQuarterly(Long userId, Long courseId);
}
