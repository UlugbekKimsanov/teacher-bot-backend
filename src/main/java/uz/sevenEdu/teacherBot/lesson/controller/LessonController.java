package uz.sevenEdu.teacherBot.lesson.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<ApiResponse<List<LessonDetailDto>>> getByCourse(
            @PathVariable Long courseId, Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(lessonService.getLessonsByCourse(courseId, userId)));
    }

    @GetMapping("/lessons/{lessonId}")
    public ResponseEntity<ApiResponse<LessonDetailDto>> getLesson(
            @PathVariable Long lessonId, Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.ok(lessonService.getLessonById(lessonId, userId)));
    }

    @PostMapping("/lessons/{lessonId}/test/submit")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> submitTest(
            @PathVariable Long lessonId,
            @RequestBody TestSubmitRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        int score = lessonService.submitTest(lessonId, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("score", score)));
    }

    @PostMapping("/lessons/{lessonId}/ask")
    public ResponseEntity<ApiResponse<Void>> askTeacher(
            @PathVariable Long lessonId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        lessonService.askTeacher(lessonId, userId, body.get("question"));
        return ResponseEntity.ok(ApiResponse.ok("Savol yuborildi", null));
    }

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
}
