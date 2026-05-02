package uz.sevenEdu.teacherBot.lesson.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.lesson.dto.LessonDetailDto;
import uz.sevenEdu.teacherBot.lesson.dto.TestSubmitRequest;
import uz.sevenEdu.teacherBot.common.enums.LessonFileType;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;
import uz.sevenEdu.teacherBot.lesson.service.LessonService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/course/{courseId}/lesson")
    public Mono<ApiResponse<List<LessonDetailDto>>> getByCourse(
            @PathVariable Long courseId, Authentication auth) {
        Long userId = getUserId(auth);
        return lessonService.getLessonsByCourse(courseId, userId).collectList().map(ApiResponse::ok);
    }

    @GetMapping("/lesson/{lessonId}")
    public Mono<ApiResponse<LessonDetailDto>> getLesson(
            @PathVariable Long lessonId, Authentication auth) {
        Long userId = getUserId(auth);
        return lessonService.getLessonById(lessonId, userId).map(ApiResponse::ok);
    }

    @PostMapping("/lesson/{lessonId}/test/submit")
    public Mono<ApiResponse<Map<String, Integer>>> submitTest(
            @PathVariable Long lessonId,
            @RequestBody TestSubmitRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return lessonService.submitTest(lessonId, userId, request)
                .map(score -> ApiResponse.ok(Map.of("score", score)));
    }

    @PostMapping("/lesson/{lessonId}/ask")
    public Mono<ApiResponse<Void>> askTeacher(
            @PathVariable Long lessonId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return lessonService.askTeacher(lessonId, userId, body.get("question"))
                .then(Mono.just(ApiResponse.ok("Savol yuborildi", null)));
    }

    @PostMapping("/lesson")
    public Mono<ApiResponse<Lesson>> create(@RequestParam Long courseId,
                                            @RequestParam String title,
                                            @RequestParam(required = false) Integer orderIndex,
                                            @RequestParam(required = false) Integer durationSec) {
        return lessonService.create(courseId, title, orderIndex, durationSec).map(ApiResponse::ok);
    }

    @PostMapping(value = "/lesson/{lessonId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<Lesson>> uploadFile(@PathVariable Long lessonId,
                                                @RequestParam LessonFileType fileType,
                                                @RequestPart("file") FilePart file) {
        return lessonService.uploadFile(lessonId, fileType, file).map(ApiResponse::ok);
    }

    private Long getUserId(Authentication auth) {
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
}
