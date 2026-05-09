package uz.sevenEdu.teacherBot.lesson.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.lesson.dto.*;

public interface LessonService {
    Flux<LessonDetailDto> getLessonsByCourse(Long courseId, Long userId);
    Mono<LessonDetailDto> getLessonById(Long lessonId, Long userId);
    Mono<Integer> submitTest(Long lessonId, Long userId, TestSubmitRequest request);
    Mono<Integer> submitExercise(Long lessonId, Long userId, ExerciseSubmitRequest request);
    Mono<Void> submitVocab(Long lessonId, Long userId, VocabSubmitRequest request);
    Mono<Void> askTeacher(Long lessonId, Long userId, String question);
}
