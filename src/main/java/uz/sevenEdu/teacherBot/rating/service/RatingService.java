package uz.sevenEdu.teacherBot.rating.service;

import uz.sevenEdu.teacherBot.rating.dto.RatingDto;

import java.util.List;

public interface RatingService {
    RatingDto.AttendanceDto getAttendance(Long userId, Long courseId);
    RatingDto.PointsSummary getPoints(Long userId);
    RatingDto.ProgressDto getProgress(Long userId, Long courseId);
    List<RatingDto.CertificateDto> getCertificates(Long userId);
}
