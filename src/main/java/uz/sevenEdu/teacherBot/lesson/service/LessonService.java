package uz.sevenEdu.teacherBot.lesson.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.enums.LessonFileType;
import uz.sevenEdu.teacherBot.lesson.dto.LessonDetailDto;
import uz.sevenEdu.teacherBot.lesson.dto.TestSubmitRequest;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;

public interface LessonService {
    Flux<LessonDetailDto> getLessonsByCourse(Long courseId, Long userId);
    Mono<LessonDetailDto> getLessonById(Long lessonId, Long userId);
    Mono<Integer> submitTest(Long lessonId, Long userId, TestSubmitRequest request);
    Mono<Void> askTeacher(Long lessonId, Long userId, String question);
    Mono<Lesson> create(Long courseId, String title, Integer orderIndex, Integer durationSec);
    Mono<Lesson> uploadFile(Long lessonId, LessonFileType fileType, FilePart filePart);
}
