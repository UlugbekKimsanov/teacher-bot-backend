package uz.sevenEdu.teacherBot.lesson.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.common.service.FileStorageService;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.lesson.dto.*;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;
import uz.sevenEdu.teacherBot.lesson.entity.TeacherQuestion;
import uz.sevenEdu.teacherBot.lesson.entity.UserLesson;
import uz.sevenEdu.teacherBot.lesson.repository.*;

import java.time.LocalDateTime;
import java.util.List;

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
    private final FileStorageService fileStorageService;
    private final uz.sevenEdu.teacherBot.rating.repository.PointsRepository pointsRepository;
    private final uz.sevenEdu.teacherBot.rating.service.AchievementService achievementService;

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

                    Mono<Long> nextLessonIdMono = lessonRepository
                            .findNextLessonInCourse(lesson.getCourseId(), lesson.getOrderIndex())
                            .map(Lesson::getId)
                            .defaultIfEmpty(0L);

                    return Mono.zip(
                            vocabularyRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList(),
                            questionRepository.findByLessonId(lessonId).collectList(),
                            exerciseRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList(),
                            ulMono,
                            nextLessonIdMono
                    ).map(tuple -> {
                        Long nextId = tuple.getT5() == 0L ? null : tuple.getT5();
                        UserLesson ul = tuple.getT4();
                        boolean completed = ul.getIsCompleted() != null && ul.getIsCompleted();
                        return LessonDetailDto.builder()
                            .id(lesson.getId())
                            .courseId(lesson.getCourseId())
                            .title(lesson.getName())
                            .coverImage(fileStorageService.toPublicUrl(lesson.getCoverImage()))
                            .videoUrl(lesson.getVideoUrl())
                            .orderIndex(lesson.getOrderIndex())
                            .durationSec(lesson.getDurationSec())
                            .completed(completed)
                            .vocabScore(ul.getVocabScore())
                            .testScore(ul.getTestScore())
                            .exerciseScore(ul.getExerciseScore())
                            .nextLessonId(completed ? nextId : null)
                            .vocabulary(tuple.getT1().stream().map(v -> LessonDetailDto.VocabDto.builder()
                                    .id(v.getId()).phraseUz(v.getTranslationUz()).phraseEn(v.getTranslationTarget()).build()).toList())
                            .questions(tuple.getT2().stream().map(q -> LessonDetailDto.QuestionDto.builder()
                                    .id(q.getId()).questionText(q.getQuestionText())
                                    .optionA(q.getOptionA()).optionB(q.getOptionB()).optionC(q.getOptionC()).build()).toList())
                            .exercises(tuple.getT3().stream().map(e -> LessonDetailDto.ExerciseDto.builder()
                                    .id(e.getId()).sentence(e.getSentence()).options(e.getOptions())
                                    .correctAnswer(e.getCorrectAnswer()).orderIndex(e.getOrderIndex()).build()).toList())
                            .build();
                    });
                });
    }

    @Override
    public Mono<SubmitResultDto> submitTest(Long lessonId, Long userId, TestSubmitRequest request) {
        return questionRepository.findByLessonId(lessonId).collectList()
                .flatMap(questions -> {
                    int correct = 0;
                    for (var q : questions) {
                        String selected = request.getAnswers().get(q.getId());
                        if (q.getCorrectOption().equalsIgnoreCase(selected)) correct++;
                    }
                    int total = questions.size();
                    int pct = total > 0 ? (int) Math.round(correct * 100.0 / total) : 0;
                    return getOrCreateUserLesson(userId, lessonId)
                            .flatMap(ul -> {
                                ul.setTestScore(pct);
                                return checkAndComplete(ul, lessonId, userId)
                                        .then(addPoints(userId, "Test — Dars", pct))
                                        .then(buildResult(pct, userId));
                            });
                });
    }

    @Override
    public Mono<SubmitResultDto> submitExercise(Long lessonId, Long userId, ExerciseSubmitRequest request) {
        return exerciseRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList()
                .flatMap(exercises -> {
                    int correct = 0;
                    for (var ex : exercises) {
                        String userAnswer = request.getAnswers().get(ex.getId());
                        if (ex.getCorrectAnswer().equalsIgnoreCase(userAnswer)) correct++;
                    }
                    int total = exercises.size();
                    int pct = total > 0 ? (int) Math.round(correct * 100.0 / total) : 0;
                    return getOrCreateUserLesson(userId, lessonId)
                            .flatMap(ul -> {
                                ul.setExerciseScore(pct);
                                return checkAndComplete(ul, lessonId, userId)
                                        .then(addPoints(userId, "Mashq — Dars", pct))
                                        .then(buildResult(pct, userId));
                            });
                });
    }

    @Override
    public Mono<SubmitResultDto> submitVocab(Long lessonId, Long userId, VocabSubmitRequest request) {
        return vocabularyRepository.findByLessonIdOrderByOrderIndex(lessonId).count()
                .flatMap(total -> {
                    int pct = total > 0 ? (int) Math.round(request.getScore() * 100.0 / total) : 0;
                    return getOrCreateUserLesson(userId, lessonId)
                            .flatMap(ul -> {
                                ul.setVocabScore(pct);
                                return checkAndComplete(ul, lessonId, userId)
                                        .then(addPoints(userId, "Lug'at — Dars", pct))
                                        .then(buildResult(pct, userId));
                            });
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
                .switchIfEmpty(Mono.defer(() ->
                        userLessonRepository.save(UserLesson.builder()
                                .userId(userId).lessonId(lessonId)
                                .isCompleted(false).build())
                        .onErrorResume(e ->
                                userLessonRepository.findByUserIdAndLessonId(userId, lessonId))
                ));
    }

    /**
     * Score'lar endi 0-100 foiz formatda saqlanadi.
     * Har bir modul kamida 50% (5 ball) bo'lganda keyingi dars ochiladi.
     * Ball = foiz / 10 (masalan 70% → 7 ball) — points jadvaliga yoziladi.
     */
    private Mono<Void> checkAndComplete(UserLesson ul, Long lessonId, Long userId) {
        // Har bir modul kamida 50% bo'lishi kerak
        boolean vocabPassed    = ul.getVocabScore()    != null && ul.getVocabScore()    >= 50;
        boolean testPassed     = ul.getTestScore()     != null && ul.getTestScore()     >= 50;
        boolean exercisePassed = ul.getExerciseScore() != null && ul.getExerciseScore() >= 50;

        boolean wasCompleted = Boolean.TRUE.equals(ul.getIsCompleted());

        if (vocabPassed && testPassed && exercisePassed && !wasCompleted) {
            ul.setIsCompleted(true);
            ul.setCompletedAt(LocalDateTime.now());
        }

        return userLessonRepository.save(ul)
                .then(Mono.defer(() -> {
                    // Har bir submit'da ball qo'shish (foiz / 10)
                    // Qaysi modul submit qilinganini aniqlash: eng oxirgi o'zgartilgan score
                    // Bu yerda oxirgi submit'ni aniqlash qiyin, shuning uchun har uchala modul uchun
                    // ball alohida qo'shiladi — submitTest, submitExercise, submitVocab da
                    return Mono.empty();
                })).then();
    }

    private Mono<Void> addPoints(Long userId, String activity, int percentScore) {
        int points = percentScore / 10; // 70% → 7 ball
        if (points <= 0) return Mono.empty();
        return pointsRepository.save(uz.sevenEdu.teacherBot.rating.entity.Points.builder()
                .userId(userId)
                .activity(activity)
                .amount(points)
                .createdAt(LocalDateTime.now())
                .build()).then();
    }

    private Mono<SubmitResultDto> buildResult(int pct, Long userId) {
        return achievementService.checkAndUnlock(userId)
                .map(achievements -> {
                    var achDtos = achievements.stream().map(a ->
                            SubmitResultDto.UnlockedAchievement.builder()
                                    .id(a.getId())
                                    .code(a.getCode())
                                    .title(a.getTitle())
                                    .description(a.getDescription())
                                    .icon(a.getIcon())
                                    .bonusPoints(a.getBonusPoints() != null ? a.getBonusPoints() : 0)
                                    .build()
                    ).toList();
                    return SubmitResultDto.from(pct, achDtos);
                });
    }

    private LessonDetailDto buildBasicDto(Lesson l, boolean unlocked, boolean completed, UserLesson ul) {
        return LessonDetailDto.builder()
                .id(l.getId()).courseId(l.getCourseId()).title(l.getName())
                .coverImage(fileStorageService.toPublicUrl(l.getCoverImage()))
                .orderIndex(l.getOrderIndex()).durationSec(l.getDurationSec())
                .locked(!unlocked).completed(completed)
                .vocabScore(ul != null ? ul.getVocabScore() : null)
                .testScore(ul != null ? ul.getTestScore() : null)
                .exerciseScore(ul != null ? ul.getExerciseScore() : null)
                .build();
    }
}
