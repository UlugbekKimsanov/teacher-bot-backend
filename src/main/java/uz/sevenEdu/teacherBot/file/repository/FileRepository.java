package uz.sevenEdu.teacherBot.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.sevenEdu.teacherBot.file.entity.FileEntity;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByLessonId(Long lessonId);
}
