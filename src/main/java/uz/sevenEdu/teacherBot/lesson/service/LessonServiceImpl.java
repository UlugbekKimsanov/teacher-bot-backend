package uz.sevenEdu.teacherBot.lesson.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.lesson.dto.LessonDetailDto;
import uz.sevenEdu.teacherBot.lesson.dto.TestSubmitRequest;
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

    private LessonDetailDto buildDetail(
            uz.sevenEdu.teacherBot.lesson.entity.Lesson lesson,
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
