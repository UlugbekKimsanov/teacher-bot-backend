package uz.sevenEdu.teacherBot.file.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.file.entity.FileEntity;

public interface FileRepository extends ReactiveCrudRepository<FileEntity, Long> {
    Flux<FileEntity> findByLessonId(Long lessonId);
}
