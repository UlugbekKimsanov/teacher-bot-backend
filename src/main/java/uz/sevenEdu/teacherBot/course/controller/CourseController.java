package uz.sevenEdu.teacherBot.course.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<ApiResponse<List<CourseDto>>> getAllCourses(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(courseService.getAllCourses(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDto>> getById(@PathVariable Long id, Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(courseService.getCourseById(id, userId)));
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<ApiResponse<CourseDto>> enroll(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.ok(courseService.enrollCourse(userId, id)));
    }

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
}
