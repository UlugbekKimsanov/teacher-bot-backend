package uz.sevenEdu.teacherBot.course.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.enums.LanguageFileType;
import uz.sevenEdu.teacherBot.course.entity.Language;

public interface LanguageService {
    Mono<Language> create(String name, String description);
    Flux<Language> getAll();
    Flux<Language> getAllAdmin();
    Mono<Language> getById(Long id);
    Mono<Language> uploadImage(Long id, LanguageFileType fileType, FilePart filePart);
}
