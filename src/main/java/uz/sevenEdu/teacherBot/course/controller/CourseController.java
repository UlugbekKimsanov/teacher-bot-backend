package uz.sevenEdu.teacherBot.course.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;
import uz.sevenEdu.teacherBot.course.entity.Course;
import uz.sevenEdu.teacherBot.course.service.CourseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public Mono<ApiResponse<List<CourseDto>>> getAllCourses(Authentication auth) {
        Long userId = getUserId(auth);
        return courseService.getAllCourses(userId).collectList().map(ApiResponse::ok);
    }

    @GetMapping("/category/{category}")
    public Mono<ApiResponse<List<CourseDto>>> getByCategory(
            @PathVariable String category, Authentication auth) {
        Long userId = getUserId(auth);
        return courseService.getCoursesByCategory(category, userId).collectList().map(ApiResponse::ok);
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

    @PostMapping
    public Mono<ApiResponse<Course>> create(@RequestParam Long languageId,
                                            @RequestParam String name,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) Integer hours,
                                            @RequestParam(required = false) Integer lessonCount,
                                            @RequestParam(required = false) String goal,
                                            @RequestParam(required = false) Boolean isPremium) {
        return courseService.create(languageId, name, category, hours, lessonCount, goal, isPremium)
                .map(ApiResponse::ok);
    }

    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<Course>> uploadCover(@PathVariable Long id,
                                                 @RequestPart("file") FilePart file) {
        return courseService.uploadCoverImage(id, file).map(ApiResponse::ok);
    }

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
}
