package uz.sevenEdu.teacherBot.course.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.enums.LanguageFileType;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.course.entity.Language;
import uz.sevenEdu.teacherBot.course.service.LanguageService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/language")
@RequiredArgsConstructor
public class LanguageController {

    private final LanguageService languageService;

    @PostMapping
    public Mono<ApiResponse<Language>> create(@RequestParam String name,
                                              @RequestParam(required = false) String description) {
        return languageService.create(name, description).map(ApiResponse::ok);
    }

    @GetMapping
    public Mono<ApiResponse<List<Language>>> getAll() {
        return languageService.getAll().collectList().map(ApiResponse::ok);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<Language>> getById(@PathVariable Long id) {
        return languageService.getById(id).map(ApiResponse::ok);
    }

    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<Language>> uploadImage(@PathVariable Long id,
                                                   @RequestParam LanguageFileType fileType,
                                                   @RequestPart("file") FilePart file) {
        return languageService.uploadImage(id, fileType, file).map(ApiResponse::ok);
    }

    @PatchMapping("/{id}/colors")
    public Mono<ApiResponse<Language>> updateColors(@PathVariable Long id,
                                                    @RequestParam String colorStart,
                                                    @RequestParam String colorEnd) {
        return languageService.updateColors(id, colorStart, colorEnd).map(ApiResponse::ok);
    }
}
