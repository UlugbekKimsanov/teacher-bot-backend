package uz.sevenEdu.teacherBot.course.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;
import uz.sevenEdu.teacherBot.course.service.CourseService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @GetMapping
    public Mono<ApiResponse<List<CourseDto>>> getAllCourses(Authentication auth) {
        Long userId = getUserId(auth);
        return courseService.getAllCourses(userId).collectList().map(ApiResponse::ok);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<CourseDto>> getById(@PathVariable Long id, Authentication auth) {
        Long userId = getUserId(auth);
        return courseService.getCourseById(id, userId).map(ApiResponse::ok);
    }

    @PostMapping("/{id}/enroll")
    public Mono<ApiResponse<CourseDto>> enroll(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return courseService.enrollCourse(userId, id).map(ApiResponse::ok);
    }

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
}
