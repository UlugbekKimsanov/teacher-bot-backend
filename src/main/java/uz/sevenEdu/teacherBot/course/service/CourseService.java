package uz.sevenEdu.teacherBot.course.service;

import uz.sevenEdu.teacherBot.course.dto.CourseDto;

import java.util.List;

public interface CourseService {
    List<CourseDto> getAllCourses(Long userId);
    CourseDto getCourseById(Long courseId, Long userId);
    CourseDto enrollCourse(Long userId, Long courseId);
}
