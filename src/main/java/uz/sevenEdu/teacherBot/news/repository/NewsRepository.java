package uz.sevenEdu.teacherBot.news.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.sevenEdu.teacherBot.news.entity.News;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {
    @Query(value = "SELECT * FROM news ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<News> findLatest(int limit);
}
