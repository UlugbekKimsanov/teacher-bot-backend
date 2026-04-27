package uz.sevenEdu.teacherBot.subject.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getAllSubjects() {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getAllSubjects()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(subjectService.getSubjectById(id)));
    }
}
