package uz.sevenEdu.teacherBot.subject.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.subject.dto.SubjectDto;
import uz.sevenEdu.teacherBot.subject.service.SubjectService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    @GetMapping
    public Mono<ApiResponse<List<SubjectDto>>> getAllSubjects() {
        return subjectService.getAllSubjects().collectList().map(ApiResponse::ok);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<SubjectDto>> getById(@PathVariable Long id) {
        return subjectService.getSubjectById(id).map(ApiResponse::ok);
    }
}
