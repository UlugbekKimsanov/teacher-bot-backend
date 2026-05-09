package uz.sevenEdu.teacherBot.rating.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.lesson.repository.UserLessonRepository;
import uz.sevenEdu.teacherBot.rating.dto.RatingDto;
import uz.sevenEdu.teacherBot.rating.repository.AttendanceRepository;
import uz.sevenEdu.teacherBot.rating.repository.CertificateRepository;
import uz.sevenEdu.teacherBot.rating.repository.PointsRepository;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {
    private final AttendanceRepository attendanceRepository;
    private final PointsRepository pointsRepository;
    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final UserLessonRepository userLessonRepository;

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
}
