package uz.sevenEdu.teacherBot.lesson.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.lesson.dto.*;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;
import uz.sevenEdu.teacherBot.lesson.entity.TeacherQuestion;
import uz.sevenEdu.teacherBot.lesson.entity.UserLesson;
import uz.sevenEdu.teacherBot.lesson.repository.*;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;
    private final QuestionRepository questionRepository;
    private final ExerciseRepository exerciseRepository;
    private final TeacherQuestionRepository teacherQuestionRepository;
    private final UserLessonRepository userLessonRepository;
    private final UserCourseRepository userCourseRepository;

    @Override
    public Flux<LessonDetailDto> getLessonsByCourse(Long courseId, Long userId) {
        return lessonRepository.findByCourseIdOrderByOrderIndex(courseId)
                .collectList()
                .flatMapMany(lessons -> {
                    if (userId == null) {
                        // Not authenticated — first lesson unlocked, rest locked
                        return Flux.fromIterable(lessons).map(l ->
                                buildBasicDto(l, l.getOrderIndex() == 1, false, null));
                    }
                    return userCourseRepository.existsByUserIdAndCourseId(userId, courseId)
                            .flatMapMany(enrolled -> userLessonRepository.findByUserId(userId)
                                    .collectList()
                                    .flatMapMany(userLessons -> Flux.fromIterable(lessons).map(lesson -> {
                                        boolean isFirst = lesson.getOrderIndex() == 1;
                                        // Find user progress for this lesson
                                        UserLesson ul = userLessons.stream()
                                                .filter(u -> u.getLessonId().equals(lesson.getId()))
                                                .findFirst().orElse(null);
                                        boolean completed = ul != null && Boolean.TRUE.equals(ul.getIsCompleted());

                                        // Unlock logic: first lesson always open; others need purchase + prev completed
                                        boolean unlocked;
                                        if (isFirst) {
                                            unlocked = true;
                                        } else if (!enrolled) {
                                            unlocked = false;
                                        } else {
                                            // Find previous lesson
                                            Lesson prevLesson = lessons.stream()
                                                    .filter(pl -> pl.getOrderIndex() == lesson.getOrderIndex() - 1)
                                                    .findFirst().orElse(null);
                                            if (prevLesson == null) {
                                                unlocked = true;
                                            } else {
                                                unlocked = userLessons.stream()
                                                        .anyMatch(u -> u.getLessonId().equals(prevLesson.getId())
                                                                && Boolean.TRUE.equals(u.getIsCompleted()));
                                            }
                                        }
                                        return buildBasicDto(lesson, unlocked, completed, ul);
                                    })));
                });
    }

    @Override
    public Mono<LessonDetailDto> getLessonById(Long lessonId, Long userId) {
        return lessonRepository.findById(lessonId)
                .switchIfEmpty(Mono.error(new NotFoundException("Dars topilmadi")))
                .flatMap(lesson -> {
                    Mono<UserLesson> ulMono = userId != null
                            ? userLessonRepository.findByUserIdAndLessonId(userId, lessonId)
                                .defaultIfEmpty(UserLesson.builder().build())
                            : Mono.just(UserLesson.builder().build());

                    return Mono.zip(
                            vocabularyRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList(),
                            questionRepository.findByLessonId(lessonId).collectList(),
                            exerciseRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList(),
                            ulMono
                    ).map(tuple -> LessonDetailDto.builder()
                            .id(lesson.getId())
                            .courseId(lesson.getCourseId())
                            .title(lesson.getName())
                            .videoUrl(lesson.getVideoUrl())
                            .orderIndex(lesson.getOrderIndex())
                            .durationSec(lesson.getDurationSec())
                            .completed(tuple.getT4().getIsCompleted() != null && tuple.getT4().getIsCompleted())
                            .vocabScore(tuple.getT4().getVocabScore())
                            .testScore(tuple.getT4().getTestScore())
                            .exerciseScore(tuple.getT4().getExerciseScore())
                            .vocabulary(tuple.getT1().stream().map(v -> LessonDetailDto.VocabDto.builder()
                                    .id(v.getId()).phraseUz(v.getTranslationUz()).phraseEn(v.getTranslationTarget()).build()).toList())
                            .questions(tuple.getT2().stream().map(q -> LessonDetailDto.QuestionDto.builder()
                                    .id(q.getId()).questionText(q.getQuestionText())
                                    .optionA(q.getOptionA()).optionB(q.getOptionB()).optionC(q.getOptionC()).build()).toList())
                            .exercises(tuple.getT3().stream().map(e -> LessonDetailDto.ExerciseDto.builder()
                                    .id(e.getId()).sentence(e.getSentence()).options(e.getOptions())
                                    .correctAnswer(e.getCorrectAnswer()).orderIndex(e.getOrderIndex()).build()).toList())
                            .build());
                });
    }

    @Override
    public Mono<Integer> submitTest(Long lessonId, Long userId, TestSubmitRequest request) {
        return questionRepository.findByLessonId(lessonId).collectList()
                .flatMap(questions -> {
                    int correct = 0;
                    for (var q : questions) {
                        String selected = request.getAnswers().get(q.getId());
                        if (q.getCorrectOption().equalsIgnoreCase(selected)) correct++;
                    }
                    int totalQuestions = questions.size();
                    int finalCorrect = correct;
                    return getOrCreateUserLesson(userId, lessonId)
                            .flatMap(ul -> {
                                ul.setTestScore(finalCorrect);
                                return checkAndComplete(ul, lessonId).thenReturn(finalCorrect);
                            });
                });
    }

    @Override
    public Mono<Integer> submitExercise(Long lessonId, Long userId, ExerciseSubmitRequest request) {
        return exerciseRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList()
                .flatMap(exercises -> {
                    int correct = 0;
                    for (var ex : exercises) {
                        String userAnswer = request.getAnswers().get(ex.getId());
                        if (ex.getCorrectAnswer().equalsIgnoreCase(userAnswer)) correct++;
                    }
                    int finalCorrect = correct;
                    return getOrCreateUserLesson(userId, lessonId)
                            .flatMap(ul -> {
                                ul.setExerciseScore(finalCorrect);
                                return checkAndComplete(ul, lessonId).thenReturn(finalCorrect);
                            });
                });
    }

    @Override
    public Mono<Void> submitVocab(Long lessonId, Long userId, VocabSubmitRequest request) {
        return getOrCreateUserLesson(userId, lessonId)
                .flatMap(ul -> {
                    ul.setVocabScore(request.getScore());
                    return checkAndComplete(ul, lessonId);
                });
    }

    @Override
    public Mono<Void> askTeacher(Long lessonId, Long userId, String question) {
        TeacherQuestion tq = TeacherQuestion.builder()
                .userId(userId).lessonId(lessonId).question(question).createdAt(LocalDateTime.now()).build();
        return teacherQuestionRepository.save(tq).then();
    }

    // ── Private helpers ──────────────────────────────────────────

    private Mono<UserLesson> getOrCreateUserLesson(Long userId, Long lessonId) {
        return userLessonRepository.findByUserIdAndLessonId(userId, lessonId)
                .switchIfEmpty(userLessonRepository.save(UserLesson.builder()
                        .userId(userId).lessonId(lessonId)
                        .isCompleted(false).vocabScore(0).testScore(0).exerciseScore(0).build()));
    }

    private Mono<Void> checkAndComplete(UserLesson ul, Long lessonId) {
        // Get totals for this lesson
        return Mono.zip(
                vocabularyRepository.findByLessonIdOrderByOrderIndex(lessonId).count(),
                questionRepository.findByLessonId(lessonId).count(),
                exerciseRepository.findByLessonIdOrderByOrderIndex(lessonId).count()
        ).flatMap(tuple -> {
            long totalVocab = tuple.getT1();
            long totalQuestions = tuple.getT2();
            long totalExercises = tuple.getT3();

            // 50% threshold for each component (at least 1 correct if there's content)
            boolean vocabPassed = totalVocab == 0 || ul.getVocabScore() >= Math.max(1, totalVocab / 2);
            boolean testPassed = totalQuestions == 0 || ul.getTestScore() >= Math.max(1, totalQuestions / 2);
            boolean exercisePassed = totalExercises == 0 || ul.getExerciseScore() >= Math.max(1, totalExercises / 2);

            if (vocabPassed && testPassed && exercisePassed) {
                ul.setIsCompleted(true);
                ul.setCompletedAt(LocalDateTime.now());
            }
            return userLessonRepository.save(ul).then();
        });
    }

    private LessonDetailDto buildBasicDto(Lesson l, boolean unlocked, boolean completed, UserLesson ul) {
        return LessonDetailDto.builder()
                .id(l.getId()).courseId(l.getCourseId()).title(l.getName())
                .orderIndex(l.getOrderIndex()).durationSec(l.getDurationSec())
                .locked(!unlocked).completed(completed)
                .vocabScore(ul != null ? ul.getVocabScore() : null)
                .testScore(ul != null ? ul.getTestScore() : null)
                .exerciseScore(ul != null ? ul.getExerciseScore() : null)
                .build();
    }
}
