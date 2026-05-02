package uz.sevenEdu.teacherBot.lesson.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.enums.LessonFileType;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.common.service.FileStorageService;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.course.repository.LanguageRepository;
import uz.sevenEdu.teacherBot.lesson.dto.LessonDetailDto;
import uz.sevenEdu.teacherBot.lesson.dto.TestSubmitRequest;
import uz.sevenEdu.teacherBot.lesson.entity.Lesson;
import uz.sevenEdu.teacherBot.lesson.entity.TeacherQuestion;
import uz.sevenEdu.teacherBot.lesson.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final VocabularyRepository vocabularyRepository;
    private final QuestionRepository questionRepository;
    private final TeacherQuestionRepository teacherQuestionRepository;
    private final CourseRepository courseRepository;
    private final LanguageRepository languageRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Flux<LessonDetailDto> getLessonsByCourse(Long courseId, Long userId) {
        return lessonRepository.findByCourseIdOrderByOrderIndex(courseId)
                .map(l -> LessonDetailDto.builder()
                        .id(l.getId())
                        .courseId(l.getCourseId())
                        .title(l.getTitle())
                        .videoUrl(l.getVideoUrl())
                        .orderIndex(l.getOrderIndex())
                        .durationSec(l.getDurationSec())
                        .build());
    }

    @Override
    public Mono<LessonDetailDto> getLessonById(Long lessonId, Long userId) {
        return lessonRepository.findById(lessonId)
                .switchIfEmpty(Mono.error(new NotFoundException("Dars topilmadi")))
                .flatMap(lesson ->
                        Mono.zip(
                                vocabularyRepository.findByLessonIdOrderByOrderIndex(lessonId).collectList(),
                                questionRepository.findByLessonId(lessonId).collectList()
                        ).map(tuple -> buildDetail(lesson, tuple.getT1(), tuple.getT2()))
                );
    }

    @Override
    public Mono<Integer> submitTest(Long lessonId, Long userId, TestSubmitRequest request) {
        return questionRepository.findByLessonId(lessonId)
                .collectList()
                .map(questions -> {
                    int correct = 0;
                    for (var q : questions) {
                        String selected = request.getAnswers().get(q.getId());
                        if (q.getCorrectOption().equalsIgnoreCase(selected)) correct++;
                    }
                    return correct;
                });
    }

    @Override
    public Mono<Void> askTeacher(Long lessonId, Long userId, String question) {
        TeacherQuestion tq = TeacherQuestion.builder()
                .userId(userId)
                .lessonId(lessonId)
                .question(question)
                .createdAt(LocalDateTime.now())
                .build();
        return teacherQuestionRepository.save(tq).then();
    }

    @Override
    public Mono<Lesson> create(Long courseId, String title, Integer orderIndex, Integer durationSec) {
        return courseRepository.findById(courseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Kurs topilmadi")))
                .flatMap(course -> languageRepository.findById(course.getLanguageId())
                        .switchIfEmpty(Mono.error(new NotFoundException("Til topilmadi")))
                        .flatMap(language -> {
                            Lesson lesson = Lesson.builder()
                                    .courseId(courseId)
                                    .title(title)
                                    .orderIndex(orderIndex != null ? orderIndex : 0)
                                    .durationSec(durationSec != null ? durationSec : 0)
                                    .build();
                            return lessonRepository.save(lesson)
                                    .doOnNext(saved -> fileStorageService.createLessonFolder(
                                            language.getName(), language.getId(),
                                            course.getName(), course.getId(),
                                            saved.getTitle(), saved.getId()));
                        }));
    }

    @Override
    public Mono<Lesson> uploadFile(Long lessonId, LessonFileType fileType, FilePart filePart) {
        return lessonRepository.findById(lessonId)
                .switchIfEmpty(Mono.error(new NotFoundException("Dars topilmadi")))
                .flatMap(lesson -> courseRepository.findById(lesson.getCourseId())
                        .flatMap(course -> languageRepository.findById(course.getLanguageId())
                                .flatMap(language -> {
                                    String oldPath = fileType == LessonFileType.VIDEO ? lesson.getVideoUrl() : lesson.getCoverImage();
                                    return fileStorageService
                                            .saveLessonFile(language.getName(), language.getId(),
                                                    course.getName(), course.getId(),
                                                    lesson.getTitle(), lesson.getId(),
                                                    fileType, filePart, oldPath)
                                            .flatMap(path -> {
                                                if (fileType == LessonFileType.VIDEO) {
                                                    lesson.setVideoUrl(path);
                                                } else {
                                                    lesson.setCoverImage(path);
                                                }
                                                return lessonRepository.save(lesson);
                                            });
                                })));
    }

    private LessonDetailDto buildDetail(
            Lesson lesson,
            List<uz.sevenEdu.teacherBot.lesson.entity.Vocabulary> vocab,
            List<uz.sevenEdu.teacherBot.lesson.entity.Question> questions) {

        return LessonDetailDto.builder()
                .id(lesson.getId())
                .courseId(lesson.getCourseId())
                .title(lesson.getTitle())
                .videoUrl(lesson.getVideoUrl())
                .orderIndex(lesson.getOrderIndex())
                .durationSec(lesson.getDurationSec())
                .vocabulary(vocab.stream().map(v -> LessonDetailDto.VocabDto.builder()
                        .id(v.getId()).phraseUz(v.getPhraseUz()).phraseEn(v.getPhraseEn()).build()
                ).toList())
                .questions(questions.stream().map(q -> LessonDetailDto.QuestionDto.builder()
                        .id(q.getId()).questionText(q.getQuestionText())
                        .optionA(q.getOptionA()).optionB(q.getOptionB()).optionC(q.getOptionC()).build()
                ).toList())
                .build();
    }
}
