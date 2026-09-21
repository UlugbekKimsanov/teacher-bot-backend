package uz.sevenEdu.teacherBot.landing.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.landing.entity.LandingContent;

public interface LandingContentRepository extends ReactiveCrudRepository<LandingContent, Long> {

    @Query("""
            INSERT INTO landing_content (id, content)
            VALUES (:id, :content)
            ON CONFLICT (id) DO UPDATE SET content = EXCLUDED.content
            RETURNING id, content
            """)
    Mono<LandingContent> upsert(Long id, String content);
}
