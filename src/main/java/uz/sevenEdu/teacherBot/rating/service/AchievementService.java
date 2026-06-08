package uz.sevenEdu.teacherBot.rating.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.lesson.repository.LessonRepository;
import uz.sevenEdu.teacherBot.lesson.repository.UserLessonRepository;
import uz.sevenEdu.teacherBot.lesson.repository.VocabularyRepository;
import uz.sevenEdu.teacherBot.rating.entity.Achievement;
import uz.sevenEdu.teacherBot.rating.entity.Points;
import uz.sevenEdu.teacherBot.rating.entity.UserAchievement;
import uz.sevenEdu.teacherBot.rating.repository.AchievementRepository;
import uz.sevenEdu.teacherBot.rating.repository.AttendanceRepository;
import uz.sevenEdu.teacherBot.rating.repository.PointsRepository;
import uz.sevenEdu.teacherBot.rating.repository.UserAchievementRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserLessonRepository userLessonRepository;
    private final PointsRepository pointsRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserCourseRepository userCourseRepository;
    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;

    /**
     * Check all achievements for a user and unlock any newly earned ones.
     * Returns list of newly unlocked achievements.
     */
    public Mono<List<Achievement>> checkAndUnlock(Long userId) {
        return achievementRepository.findAll().collectList()
                .flatMap(allAchievements ->
                    userAchievementRepository.findByUserId(userId).collectList()
                        .flatMap(existing -> {
                            // IDs of already unlocked achievements
                            var unlockedIds = existing.stream()
                                    .map(UserAchievement::getAchievementId)
                                    .collect(java.util.stream.Collectors.toSet());

                            // Filter to not-yet-unlocked
                            var candidates = allAchievements.stream()
                                    .filter(a -> !unlockedIds.contains(a.getId()))
                                    .toList();

                            if (candidates.isEmpty()) return Mono.just(List.<Achievement>of());

                            // Gather user stats
                            return gatherStats(userId).flatMap(stats ->
                                {
                                    var newlyUnlocked = candidates.stream()
                                            .filter(a -> meetsCondition(a, stats))
                                            .toList();

                                    if (newlyUnlocked.isEmpty()) return Mono.just(List.<Achievement>of());

                                    // Save all new user_achievements + bonus points
                                    return Flux.fromIterable(newlyUnlocked)
                                            .flatMap(a -> {
                                                var ua = UserAchievement.builder()
                                                        .userId(userId)
                                                        .achievementId(a.getId())
                                                        .unlockedAt(LocalDateTime.now())
                                                        .build();
                                                Mono<Void> saveMono = userAchievementRepository.save(ua).then();
                                                if (a.getBonusPoints() != null && a.getBonusPoints() > 0) {
                                                    saveMono = saveMono.then(
                                                            pointsRepository.save(Points.builder()
                                                                    .userId(userId)
                                                                    .activity("Yutuq: " + a.getTitle())
                                                                    .amount(a.getBonusPoints())
                                                                    .createdAt(LocalDateTime.now())
                                                                    .build()).then()
                                                    );
                                                }
                                                return saveMono.thenReturn(a);
                                            })
                                            .collectList();
                                }
                            );
                        })
                );
    }

    /**
     * Get all achievements with unlock status for a user.
     */
    public Mono<List<AchievementDto>> getAllForUser(Long userId) {
        return Mono.zip(
                achievementRepository.findAll().collectList(),
                userAchievementRepository.findByUserId(userId).collectList()
        ).map(tuple -> {
            var all = tuple.getT1();
            var unlocked = tuple.getT2();
            var unlockedMap = unlocked.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            UserAchievement::getAchievementId,
                            UserAchievement::getUnlockedAt));

            return all.stream().map(a -> AchievementDto.builder()
                    .id(a.getId())
                    .code(a.getCode())
                    .title(a.getTitle())
                    .description(a.getDescription())
                    .icon(a.getIcon())
                    .bonusPoints(a.getBonusPoints())
                    .unlocked(unlockedMap.containsKey(a.getId()))
                    .unlockedAt(unlockedMap.get(a.getId()))
                    .build()
            ).toList();
        });
    }

    // ── Stats gathering ──────────────────────────────────

    private Mono<UserStats> gatherStats(Long userId) {
        var lessonsCompleted = userLessonRepository.findByUserId(userId).collectList()
                .map(list -> list.stream().filter(ul -> Boolean.TRUE.equals(ul.getIsCompleted())).count());

        // Haqiqiy o'rganilgan so'zlar soni (vocab_score >= 50% bo'lgan darslardagi so'zlar)
        var vocabTotal = vocabularyRepository.countLearnedByUserId(userId);

        var perfectTests = userLessonRepository.findByUserId(userId).collectList()
                .map(list -> list.stream()
                        .filter(ul -> ul.getTestScore() != null && ul.getTestScore() == 100)
                        .count());

        var totalPoints = pointsRepository.sumByUserId(userId);

        // Joriy streak hisoblash (ketma-ket kunlar)
        var currentStreak = attendanceRepository.findDistinctDatesByUserId(userId)
                .collectList()
                .map(dates -> {
                    if (dates.isEmpty()) return 0L;
                    LocalDate today = LocalDate.now();
                    LocalDate expected = today;
                    if (!dates.contains(today) && dates.contains(today.minusDays(1))) {
                        expected = today.minusDays(1);
                    }
                    long streak = 0;
                    for (LocalDate d : dates) {
                        if (d.equals(expected)) {
                            streak++;
                            expected = expected.minusDays(1);
                        } else if (d.isBefore(expected)) {
                            break;
                        }
                    }
                    return streak;
                });

        // Yakunlangan kurslar soni (barcha darslari completed bo'lgan kurslar)
        var coursesCompleted = userCourseRepository.findByUserId(userId)
                .flatMap(uc -> Mono.zip(
                        lessonRepository.countByCourseId(uc.getCourseId()),
                        userLessonRepository.countCompletedByUserAndCourse(userId, uc.getCourseId())
                ).filter(t -> t.getT1() > 0 && t.getT2() >= t.getT1()))
                .count();

        return Mono.zip(lessonsCompleted, vocabTotal, perfectTests, totalPoints, currentStreak, coursesCompleted)
                .map(t -> new UserStats(t.getT1(), t.getT2(), t.getT3(), t.getT4(), t.getT5(), t.getT6()));
    }

    private boolean meetsCondition(Achievement a, UserStats stats) {
        int val = a.getConditionValue();
        return switch (a.getConditionType()) {
            case "lessons_completed" -> stats.lessonsCompleted >= val;
            case "vocab_total" -> stats.vocabTotal >= val;
            case "perfect_test" -> stats.perfectTests >= val;
            case "total_points" -> stats.totalPoints >= val;
            case "courses_completed" -> stats.coursesCompleted >= val;
            case "streak_days" -> stats.currentStreak >= val;
            default -> false;
        };
    }

    private record UserStats(long lessonsCompleted, long vocabTotal, long perfectTests,
                             long totalPoints, long currentStreak, long coursesCompleted) {}

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AchievementDto {
        private Long id;
        private String code;
        private String title;
        private String description;
        private String icon;
        private Integer bonusPoints;
        private boolean unlocked;
        private LocalDateTime unlockedAt;
    }
}
