package uz.sevenEdu.teacherBot.rating.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    public RatingDto.AttendanceDto getAttendance(Long userId, Long courseId) {
        String courseName = courseRepository.findById(courseId)
                .map(c -> c.getName()).orElse("Kurs");
        return RatingDto.AttendanceDto.builder()
                .courseName(courseName)
                .weeklyMissed(attendanceRepository.countWeekly(userId, courseId))
                .monthlyMissed(attendanceRepository.countMonthly(userId, courseId))
                .quarterlyMissed(attendanceRepository.countQuarterly(userId, courseId))
                .build();
    }

    @Override
    public RatingDto.PointsSummary getPoints(Long userId) {
        var entries = pointsRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(p -> RatingDto.PointsSummary.PointEntry.builder()
                        .activity(p.getActivity()).amount(p.getAmount()).build())
                .toList();
        return RatingDto.PointsSummary.builder()
                .entries(entries)
                .total(pointsRepository.sumByUserId(userId))
                .build();
    }

    @Override
    public RatingDto.ProgressDto getProgress(Long userId, Long courseId) {
        String courseName = courseRepository.findById(courseId)
                .map(c -> c.getName()).orElse("Kurs");
        return RatingDto.ProgressDto.builder()
                .courseName(courseName)
                .vocabularyScore(80)
                .testScore(100)
                .questionsScore(40)
                .maxScore(100)
                .build();
    }

    @Override
    public List<RatingDto.CertificateDto> getCertificates(Long userId) {
        return certificateRepository.findByUserId(userId).stream()
                .map(cert -> {
                    String courseName = courseRepository.findById(cert.getCourseId())
                            .map(c -> c.getName()).orElse("Kurs");
                    return RatingDto.CertificateDto.builder()
                            .id(cert.getId())
                            .courseId(cert.getCourseId())
                            .courseName(courseName)
                            .issuedAt(cert.getIssuedAt() != null ? cert.getIssuedAt().toString() : null)
                            .build();
                })
                .toList();
    }
}
