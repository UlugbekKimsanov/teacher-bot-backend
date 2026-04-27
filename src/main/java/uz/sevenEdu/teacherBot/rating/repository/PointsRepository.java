package uz.sevenEdu.teacherBot.rating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.sevenEdu.teacherBot.rating.entity.Points;

import java.util.List;

public interface PointsRepository extends JpaRepository<Points, Long> {
    List<Points> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query(value = "SELECT COALESCE(SUM(amount),0) FROM points WHERE user_id = :userId", nativeQuery = true)
    long sumByUserId(Long userId);
}
