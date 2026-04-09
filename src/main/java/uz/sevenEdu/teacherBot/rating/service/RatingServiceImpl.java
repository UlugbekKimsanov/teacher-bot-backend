package uz.sevenEdu.teacherBot.rating.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.rating.dto.RatingDto;
import uz.sevenEdu.teacherBot.rating.repository.AttendanceRepository;
import uz.sevenEdu.teacherBot.rating.repository.CertificateRepository;
import uz.sevenEdu.teacherBot.rating.repository.PointsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final AttendanceRepository attendanceRepository;
    private final PointsRepository pointsRepository;
    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;

    @Override
    public Mono<RatingDto.AttendanceDto> getAttendance(Long userId, Long courseId) {
        return Mono.zip(
                attendanceRepository.countWeekly(userId, courseId),
                attendanceRepository.countMonthly(userId, courseId),
                attendanceRepository.countQuarterly(userId, courseId),
                courseRepository.findById(courseId).map(c -> c.getName()).defaultIfEmpty("Kurs")
        ).map(t -> RatingDto.AttendanceDto.builder()
                .courseName(t.getT4())
                .weeklyMissed(t.getT1())
                .monthlyMissed(t.getT2())
                .quarterlyMissed(t.getT3())
                .build());
    }

    @Override
    public Mono<RatingDto.PointsSummary> getPoints(Long userId) {
        return Mono.zip(
                pointsRepository.findByUserIdOrderByCreatedAtDesc(userId)
                        .map(p -> RatingDto.PointsSummary.PointEntry.builder()
                                .activity(p.getActivity()).amount(p.getAmount()).build())
                        .collectList(),
                pointsRepository.sumByUserId(userId)
        ).map(t -> RatingDto.PointsSummary.builder()
                .entries(t.getT1())
                .total(t.getT2())
                .build());
    }

    @Override
    public Mono<RatingDto.ProgressDto> getProgress(Long userId, Long courseId) {
        return courseRepository.findById(courseId)
                .map(c -> RatingDto.ProgressDto.builder()
                        .courseName(c.getName())
                        .vocabularyScore(80)
                        .testScore(100)
                        .questionsScore(40)
                        .maxScore(100)
                        .build())
                .defaultIfEmpty(RatingDto.ProgressDto.builder()
                        .courseName("Kurs")
                        .vocabularyScore(0).testScore(0).questionsScore(0).maxScore(100)
                        .build());
    }

    @Override
    public Flux<RatingDto.CertificateDto> getCertificates(Long userId) {
        return certificateRepository.findByUserId(userId)
                .flatMap(cert -> courseRepository.findById(cert.getCourseId())
                        .map(course -> RatingDto.CertificateDto.builder()
                                .id(cert.getId())
                                .courseId(cert.getCourseId())
                                .courseName(course.getName())
                                .issuedAt(cert.getIssuedAt().toString())
                                .build()));
    }
}
