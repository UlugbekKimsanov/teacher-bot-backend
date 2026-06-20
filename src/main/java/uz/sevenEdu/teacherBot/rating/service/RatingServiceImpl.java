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
                pointsRepository.findByUserIdOrderByCreatedAtDesc(userId)
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
        return userRepository.findByRole("STUDENT")
                .filter(user -> !Boolean.TRUE.equals(user.getIsGuest()))
                .flatMap(user -> pointsRepository.sumByUserId(user.getId())
                        .map(total -> Map.entry(user, total.intValue())))
                .collectSortedList((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(sorted -> {
                    List<RatingDto.LeaderboardDto.LeaderboardEntry> entries = new ArrayList<>();
                    int myRank = 0;
                    for (int i = 0; i < sorted.size() && i < 50; i++) {
                        var entry = sorted.get(i);
                        BaseUser u = entry.getKey();
                        boolean isMe = u.getId().equals(currentUserId);
                        if (isMe) myRank = i + 1;
                        String initials = ((u.getFirstName() != null ? u.getFirstName().substring(0, 1) : "") +
                                (u.getLastName() != null ? u.getLastName().substring(0, 1) : "")).toUpperCase();
                        entries.add(RatingDto.LeaderboardDto.LeaderboardEntry.builder()
                                .rank(i + 1)
                                .name((u.getFirstName() != null ? u.getFirstName() : "") + " " +
                                      (u.getLastName() != null ? u.getLastName() : ""))
                                .points(entry.getValue())
                                .isMe(isMe)
                                .avatarInitials(initials)
                                .build());
                    }
                    // If user not in top 50, find their rank
                    if (myRank == 0) {
                        for (int i = 0; i < sorted.size(); i++) {
                            if (sorted.get(i).getKey().getId().equals(currentUserId)) {
                                myRank = i + 1;
                                break;
                            }
                        }
                    }
                    return RatingDto.LeaderboardDto.builder()
                            .entries(entries)
                            .myRank(myRank)
                            .build();
                });
    }

    @Override
    public Mono<RatingDto.DailyGoalsDto> getDailyGoals(Long userId) {
        Mono<Long> lessonsDone = userLessonRepository.findByUserId(userId)
                .filter(ul -> Boolean.TRUE.equals(ul.getIsCompleted()) &&
                        ul.getCompletedAt() != null &&
                        ul.getCompletedAt().toLocalDate().equals(LocalDate.now()))
                .count();

        Mono<Long> todayPoints = pointsRepository.sumTodayByUserId(userId);

        // Bugun yakunlangan darslardagi haqiqiy so'zlar soni
        Mono<Long> todayWords = vocabularyRepository.countTodayLearnedByUserId(userId);

        return Mono.zip(lessonsDone, todayPoints, todayWords)
                .map(tuple -> {
                    int lessons = tuple.getT1().intValue();
                    int points = tuple.getT2().intValue();
                    int words = tuple.getT3().intValue();
                    return RatingDto.DailyGoalsDto.builder()
                            .lessonsGoal(3)
                            .lessonsDone(lessons)
                            .minutesGoal(30)
                            .minutesDone(lessons * 6) // ~6 min per lesson (vaqt kuzatuvi yo'q)
                            .wordsGoal(20)
                            .wordsDone(words) // haqiqiy so'z soni
                            .build();
                });
    }
}
