package uz.sevenEdu.teacherBot.lesson.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.lesson.dto.LessonDetailDto;
import uz.sevenEdu.teacherBot.lesson.dto.TestSubmitRequest;

public interface LessonService {
    Flux<LessonDetailDto> getLessonsByCourse(Long courseId, Long userId);
    Mono<LessonDetailDto> getLessonById(Long lessonId, Long userId);
    Mono<Integer> submitTest(Long lessonId, Long userId, TestSubmitRequest request);
    Mono<Void> askTeacher(Long lessonId, Long userId, String question);
}
