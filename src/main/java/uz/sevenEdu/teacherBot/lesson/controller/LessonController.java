package uz.sevenEdu.teacherBot.lesson.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.lesson.dto.LessonDetailDto;
import uz.sevenEdu.teacherBot.lesson.dto.TestSubmitRequest;
import uz.sevenEdu.teacherBot.lesson.service.LessonService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;

    @GetMapping("/courses/{courseId}/lessons")
    public Mono<ApiResponse<List<LessonDetailDto>>> getByCourse(@PathVariable Long courseId, Authentication auth) {
        Long userId = getUserId(auth);
        return lessonService.getLessonsByCourse(courseId, userId).collectList().map(ApiResponse::ok);
    }

    @GetMapping("/lessons/{lessonId}")
    public Mono<ApiResponse<LessonDetailDto>> getLesson(@PathVariable Long lessonId, Authentication auth) {
        Long userId = getUserId(auth);
        return lessonService.getLessonById(lessonId, userId).map(ApiResponse::ok);
    }

    @PostMapping("/lessons/{lessonId}/test/submit")
    public Mono<ApiResponse<Map<String, Integer>>> submitTest(@PathVariable Long lessonId, @RequestBody TestSubmitRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return lessonService.submitTest(lessonId, userId, request).map(score -> ApiResponse.ok(Map.of("score", score)));
    }

    @PostMapping("/lessons/{lessonId}/ask")
    public Mono<ApiResponse<Void>> askTeacher(@PathVariable Long lessonId, @RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return lessonService.askTeacher(lessonId, userId, body.get("question")).then(Mono.just(ApiResponse.ok("Savol yuborildi", null)));
    }

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
}
