package uz.sevenEdu.teacherBot.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.enums.LanguageFileType;
import uz.sevenEdu.teacherBot.common.exception.BadRequestException;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.common.service.FileStorageService;
import uz.sevenEdu.teacherBot.course.entity.Language;
import uz.sevenEdu.teacherBot.course.repository.LanguageRepository;

@Service
@RequiredArgsConstructor
public class LanguageServiceImpl implements LanguageService {

    private final LanguageRepository languageRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Mono<Language> create(String name, String description) {
        return languageRepository.findByName(name)
                .flatMap(existing -> Mono.<Language>error(
                        new BadRequestException("'" + name + "' tili allaqachon mavjud")))
                .switchIfEmpty(Mono.defer(() -> {
                    Language language = Language.builder()
                            .name(name)
                            .description(description)
                            .build();
                    return languageRepository.save(language);
                }))
                .doOnNext(lang -> fileStorageService.createLanguageFolder(lang.getName(), lang.getId()));
    }

    @Override
    public Flux<Language> getAll() {
        return languageRepository.findAll()
                .map(this::resolveImageUrls);
    }

    @Override
    public Mono<Language> getById(Long id) {
        return languageRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Til topilmadi")))
                .map(this::resolveImageUrls);
    }

    private Language resolveImageUrls(Language lang) {
        lang.setFlagImage(fileStorageService.toPublicUrl(lang.getFlagImage()));
        lang.setBackgroundImage(fileStorageService.toPublicUrl(lang.getBackgroundImage()));
        return lang;
    }

    @Override
    public Mono<Language> updateColors(Long id, String colorStart, String colorEnd) {
        return languageRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Til topilmadi")))
                .flatMap(lang -> {
                    lang.setColorStart(colorStart);
                    lang.setColorEnd(colorEnd);
                    return languageRepository.save(lang);
                });
    }

    @Override
    public Mono<Language> uploadImage(Long id, LanguageFileType fileType, FilePart filePart) {
        return languageRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Til topilmadi")))
                .flatMap(lang -> {
                    String oldPath = fileType == LanguageFileType.FLAG ? lang.getFlagImage() : lang.getBackgroundImage();
                    return fileStorageService
                            .saveLanguageImage(lang.getName(), lang.getId(), fileType, filePart, oldPath)
                            .flatMap(path -> {
                                if (fileType == LanguageFileType.FLAG) {
                                    lang.setFlagImage(path);
                                } else {
                                    lang.setBackgroundImage(path);
                                }
                                return languageRepository.save(lang);
                            });
                });
    }
}
