package uz.sevenEdu.teacherBot.news.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.news.entity.News;

public interface NewsRepository extends ReactiveCrudRepository<News, Long> {
    @Query("SELECT * FROM news ORDER BY created_at DESC LIMIT :limit")
    Flux<News> findLatest(int limit);
}
