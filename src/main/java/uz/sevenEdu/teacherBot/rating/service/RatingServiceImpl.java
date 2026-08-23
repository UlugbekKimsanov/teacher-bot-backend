package uz.sevenEdu.teacherBot.rating.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.course.repository.UserCourseRepository;
import uz.sevenEdu.teacherBot.lesson.repository.UserLessonRepository;
import uz.sevenEdu.teacherBot.rating.dto.RatingDto;
import uz.sevenEdu.teacherBot.rating.repository.AttendanceRepository;
import uz.sevenEdu.teacherBot.rating.repository.CertificateRepository;
import uz.sevenEdu.teacherBot.rating.repository.PointsRepository;
import uz.sevenEdu.teacherBot.user.entity.BaseUser;
import uz.sevenEdu.teacherBot.user.enums.UserRole;
import uz.sevenEdu.teacherBot.user.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {
    private final AttendanceRepository attendanceRepository;
    private final PointsRepository pointsRepository;
    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final UserLessonRepository userLessonRepository;
    private final UserCourseRepository userCourseRepository;
    private final UserRepository userRepository;
    private final uz.sevenEdu.teacherBot.lesson.repository.VocabularyRepository vocabularyRepository;
    private final uz.sevenEdu.teacherBot.rating.repository.UserGoalSettingsRepository goalSettingsRepository;
    private final uz.sevenEdu.teacherBot.rating.repository.DailyActivityRepository dailyActivityRepository;
    private final uz.sevenEdu.teacherBot.rating.repository.DailyGoalHistoryRepository goalHistoryRepository;

    @Override
    public Mono<RatingDto.AttendanceDto> getAttendance(Long userId, Long courseId) {
        Mono<String> nameMono = courseRepository.findById(courseId)
                .map(Course::getName).defaultIfEmpty("Kurs");
        return nameMono.flatMap(courseName ->
                Mono.zip(
                        attendanceRepository.countWeekly(userId, courseId),
                        attendanceRepository.countMonthly(userId, courseId),
                        attendanceRepository.countQuarterly(userId, courseId)
                ).map(tuple -> RatingDto.AttendanceDto.builder()
                        .courseName(courseName)
                        .weeklyMissed(tuple.getT1())
                        .monthlyMissed(tuple.getT2())
                        .quarterlyMissed(tuple.getT3())
                        .build()));
    }

    @Override
    public Mono<RatingDto.PointsSummary> getPoints(Long userId) {
        Mono<java.util.List<RatingDto.PointsSummary.PointEntry>> entriesMono =
                pointsRepository.findTop100ByUserIdOrderByCreatedAtDesc(userId)
                        .map(p -> RatingDto.PointsSummary.PointEntry.builder()
                                .activity(p.getActivity()).amount(p.getAmount()).build())
                        .collectList();
        Mono<Long> totalMono = pointsRepository.sumByUserId(userId);
        return Mono.zip(entriesMono, totalMono)
                .map(tuple -> RatingDto.PointsSummary.builder()
                        .entries(tuple.getT1()).total(tuple.getT2()).build());
    }

    @Override
    public Mono<RatingDto.ProgressDto> getProgress(Long userId, Long courseId) {
        return courseRepository.findById(courseId)
                .map(Course::getName).defaultIfEmpty("Kurs")
                .flatMap(courseName ->
                        userLessonRepository.findByUserIdAndCourseId(userId, courseId)
                                .collectList()
                                .map(userLessons -> {
                                    int vocabTotal = userLessons.stream()
                                            .mapToInt(ul -> ul.getVocabScore() != null ? ul.getVocabScore() : 0)
                                            .sum();
                                    int testTotal = userLessons.stream()
                                            .mapToInt(ul -> ul.getTestScore() != null ? ul.getTestScore() : 0)
                                            .sum();
                                    int exerciseTotal = userLessons.stream()
                                            .mapToInt(ul -> ul.getExerciseScore() != null ? ul.getExerciseScore() : 0)
                                            .sum();
                                    int completedCount = (int) userLessons.stream()
                                            .filter(ul -> Boolean.TRUE.equals(ul.getIsCompleted()))
                                            .count();
                                    int maxScore = Math.max(userLessons.size(), 1) * 10;
                                    return RatingDto.ProgressDto.builder()
                                            .courseName(courseName)
                                            .vocabularyScore(vocabTotal)
                                            .testScore(testTotal)
                                            .questionsScore(exerciseTotal)
                                            .maxScore(maxScore)
                                            .build();
                                }));
    }

    @Override
    public Flux<RatingDto.CertificateDto> getCertificates(Long userId) {
        return certificateRepository.findByUserId(userId)
                .flatMap(cert -> courseRepository.findById(cert.getCourseId())
                        .map(Course::getName).defaultIfEmpty("Kurs")
                        .map(courseName -> RatingDto.CertificateDto.builder()
                                .id(cert.getId())
                                .courseId(cert.getCourseId())
                                .courseName(courseName)
                                .issuedAt(cert.getIssuedAt() != null ? cert.getIssuedAt().toString() : null)
                                .build()));
    }

    @Override
    public Mono<RatingDto.StreakDto> getStreak(Long userId) {
        return attendanceRepository.findDistinctDatesByUserId(userId)
                .collectList()
                .map(dates -> {
                    // dates are sorted DESC
                    int currentStreak = 0;
                    int longestStreak = 0;
                    int tempStreak = 0;

                    // Calculate current streak (consecutive days ending today or yesterday)
                    LocalDate today = LocalDate.now();
                    if (!dates.isEmpty()) {
                        LocalDate expected = today;
                        // Allow streak to start from today or yesterday
                        if (!dates.contains(today) && dates.contains(today.minusDays(1))) {
                            expected = today.minusDays(1);
                        }
                        for (LocalDate d : dates) {
                            if (d.equals(expected)) {
                                currentStreak++;
                                expected = expected.minusDays(1);
                            } else if (d.isBefore(expected)) {
                                break;
                            }
                        }
                    }

                    // Calculate longest streak
                    List<LocalDate> sorted = new ArrayList<>(dates);
                    sorted.sort(Comparator.naturalOrder());
                    for (int i = 0; i < sorted.size(); i++) {
                        if (i == 0 || sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                            tempStreak++;
                        } else {
                            tempStreak = 1;
                        }
                        longestStreak = Math.max(longestStreak, tempStreak);
                    }

                    // Last 30 studied days as ISO strings
                    LocalDate thirtyAgo = today.minusDays(30);
                    List<String> studiedDays = dates.stream()
                            .filter(d -> !d.isBefore(thirtyAgo))
                            .map(LocalDate::toString)
                            .collect(Collectors.toList());

                    // Week days bitmask [Mon..Sun]
                    LocalDate monday = today.with(DayOfWeek.MONDAY);
                    Set<LocalDate> dateSet = new HashSet<>(dates);
                    List<Boolean> weekDays = new ArrayList<>(7);
                    for (int i = 0; i < 7; i++) {
                        weekDays.add(dateSet.contains(monday.plusDays(i)));
                    }

                    return RatingDto.StreakDto.builder()
                            .currentStreak(currentStreak)
                            .longestStreak(longestStreak)
                            .studiedDays(studiedDays)
                            .weekDays(weekDays)
                            .build();
                });
    }

    @Override
    public Mono<Void> recordAttendance(Long userId) {
        // Mehmon — davomat/streak qayd etilmaydi
        if (uz.sevenEdu.teacherBot.common.util.GuestUtil.isGuest(userId)) return Mono.empty();
        return userCourseRepository.findByUserId(userId)
                .flatMap(uc -> attendanceRepository.recordToday(userId, uc.getCourseId()))
                .then();
    }

    @Override
    public Mono<RatingDto.LeaderboardDto> getLeaderboard(Long currentUserId) {
        // N+1 o'rniga bitta GROUP BY agregat so'rov (top-50).
        return pointsRepository.findTop50Leaderboard().collectList()
                .flatMap(rows -> {
                    List<RatingDto.LeaderboardDto.LeaderboardEntry> entries = new ArrayList<>();
                    int myRank = 0;
                    for (int i = 0; i < rows.size(); i++) {
                        var r = rows.get(i);
                        boolean isMe = r.getId() != null && r.getId().equals(currentUserId);
                        if (isMe) myRank = i + 1;
                        String fn = r.getFirstName() != null ? r.getFirstName() : "";
                        String ln = r.getLastName() != null ? r.getLastName() : "";
                        String initials = ((fn.isEmpty() ? "" : fn.substring(0, 1)) +
                                (ln.isEmpty() ? "" : ln.substring(0, 1))).toUpperCase();
                        entries.add(RatingDto.LeaderboardDto.LeaderboardEntry.builder()
                                .rank(i + 1)
                                .name((fn + " " + ln).trim())
                                .points(r.getTotal() != null ? r.getTotal().intValue() : 0)
                                .isMe(isMe)
                                .avatarInitials(initials)
                                .build());
                    }
                    if (myRank > 0) {
                        return Mono.just(RatingDto.LeaderboardDto.builder()
                                .entries(entries).myRank(myRank).build());
                    }
                    // Top-50 dan tashqarida — rankni alohida (2 ta yengil so'rov) hisoblaymiz.
                    return pointsRepository.sumByUserId(currentUserId).defaultIfEmpty(0L)
                            .flatMap(myTotal -> pointsRepository.countRankedAbove(myTotal)
                                    .map(above -> RatingDto.LeaderboardDto.builder()
                                            .entries(entries)
                                            .myRank(above.intValue() + 1)
                                            .build()));
                });
    }

    @Override
    public Mono<RatingDto.DailyGoalsDto> getDailyGoals(Long userId) {
        return buildTodayGoals(userId);
    }

    /** Bugungi jonli maqsad: daqiqa daily_activity'dan (sekund/60), so'z vocab'dan,
     *  maqsadlar user_goal_settings'dan (yo'q bo'lsa 30/20). */
    private Mono<RatingDto.DailyGoalsDto> buildTodayGoals(Long userId) {
        Mono<Long> lessonsDone = userLessonRepository.findByUserId(userId)
                .filter(ul -> Boolean.TRUE.equals(ul.getIsCompleted()) &&
                        ul.getCompletedAt() != null &&
                        ul.getCompletedAt().toLocalDate().equals(LocalDate.now()))
                .count();
        Mono<Integer> todaySeconds = dailyActivityRepository.todaySeconds(userId).defaultIfEmpty(0);
        Mono<Long> todayWords = vocabularyRepository.countTodayLearnedByUserId(userId);
        Mono<int[]> goalsMono = goalSettingsRepository.findById(userId)
                .map(s -> new int[]{
                        s.getMinutesGoal() != null ? s.getMinutesGoal() : 30,
                        s.getWordsGoal() != null ? s.getWordsGoal() : 20})
                .defaultIfEmpty(new int[]{30, 20});

        return Mono.zip(lessonsDone, todaySeconds, todayWords, goalsMono)
                .map(t -> {
                    int lessons = t.getT1().intValue();
                    int minutes = t.getT2() / 60;
                    int words = t.getT3().intValue();
                    int[] g = t.getT4();
                    return RatingDto.DailyGoalsDto.builder()
                            .lessonsGoal(3)
                            .lessonsDone(lessons)
                            .minutesGoal(g[0])
                            .minutesDone(minutes)
                            .wordsGoal(g[1])
                            .wordsDone(words)
                            .build();
                });
    }

    /** Foiz (0..100): daqiqa% va so'z% ning o'rta arifmetigi. */
    private static double goalPercent(int minutesDone, int minutesGoal, int wordsDone, int wordsGoal) {
        double mp = minutesGoal > 0 ? Math.min(1.0, (double) minutesDone / minutesGoal) : 0;
        double wp = wordsGoal > 0 ? Math.min(1.0, (double) wordsDone / wordsGoal) : 0;
        return Math.round(((mp + wp) / 2.0) * 100.0);
    }

    @Override
    public Mono<Void> recordActivity(Long userId, int seconds) {
        if (seconds <= 0) return Mono.empty();
        if (uz.sevenEdu.teacherBot.common.util.GuestUtil.isGuest(userId)) return Mono.empty();
        return dailyActivityRepository.addSecondsToday(userId, seconds).then();
    }

    @Override
    public Mono<RatingDto.DailyGoalsDto> updateGoals(Long userId, int minutesGoal, int wordsGoal) {
        int mg = minutesGoal > 0 ? minutesGoal : 30;
        int wg = wordsGoal > 0 ? wordsGoal : 20;
        return goalSettingsRepository.upsert(userId, mg, wg).then(buildTodayGoals(userId));
    }

    @Override
    public Mono<RatingDto.GoalsHistoryDto> getGoalsHistory(Long userId) {
        Mono<RatingDto.DailyGoalsDto> todayMono = buildTodayGoals(userId);
        Mono<List<RatingDto.DailyGoalRow>> historyMono = goalHistoryRepository.findRecentByUserId(userId)
                // bugungi kun cron yozgan bo'lsa ham — uni jonlisi bilan almashtiramiz
                .filter(h -> h.getHistoryDate() == null || !h.getHistoryDate().equals(LocalDate.now()))
                .map(h -> RatingDto.DailyGoalRow.builder()
                        .date(h.getHistoryDate() != null ? h.getHistoryDate().toString() : "")
                        .minutesDone(h.getMinutesDone() != null ? h.getMinutesDone() : 0)
                        .wordsDone(h.getWordsDone() != null ? h.getWordsDone() : 0)
                        .minutesGoal(h.getMinutesGoal() != null ? h.getMinutesGoal() : 0)
                        .wordsGoal(h.getWordsGoal() != null ? h.getWordsGoal() : 0)
                        .percent(h.getPercent() != null ? h.getPercent() : 0)
                        .build())
                .collectList();
        return Mono.zip(todayMono, historyMono).map(t -> {
            RatingDto.DailyGoalsDto today = t.getT1();
            List<RatingDto.DailyGoalRow> rows = new ArrayList<>();
            double todayPercent = goalPercent(today.getMinutesDone(), today.getMinutesGoal(),
                    today.getWordsDone(), today.getWordsGoal());
            rows.add(RatingDto.DailyGoalRow.builder()
                    .date(LocalDate.now().toString())
                    .minutesDone(today.getMinutesDone())
                    .wordsDone(today.getWordsDone())
                    .minutesGoal(today.getMinutesGoal())
                    .wordsGoal(today.getWordsGoal())
                    .percent(todayPercent)
                    .build());
            rows.addAll(t.getT2()); // tarix (bugundan tashqari), sana bo'yicha kamayuvchi
            double avg = rows.isEmpty() ? 0
                    : Math.round(rows.stream().mapToDouble(RatingDto.DailyGoalRow::getPercent).average().orElse(0));
            return RatingDto.GoalsHistoryDto.builder()
                    .averagePercent(avg)
                    .today(today)
                    .days(rows)
                    .build();
        });
    }

    @Override
    public Mono<Void> snapshotAllDailyGoals() {
        // Set-based: barcha real o'quvchilar uchun bitta SQL (~500K so'rov o'rniga 1).
        return goalHistoryRepository.snapshotAllToday().then();
    }
}
