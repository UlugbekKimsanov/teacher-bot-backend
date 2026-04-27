package uz.sevenEdu.teacherBot.lesson.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
    public List<LessonDetailDto> getLessonsByCourse(Long courseId, Long userId) {
        return lessonRepository.findByCourseIdOrderByOrderIndex(courseId).stream()
                .map(l -> LessonDetailDto.builder()
                        .id(l.getId())
                        .courseId(l.getCourse() != null ? l.getCourse().getId() : null)
                        .title(l.getName())
                        .orderIndex(l.getOrderIndex())
                        .durationSec(l.getDurationSec())
                        .build())
                .toList();
    }

    @Override
    public LessonDetailDto getLessonById(Long lessonId, Long userId) {
        var lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Dars topilmadi"));
        var vocab = vocabularyRepository.findByLessonIdOrderByOrderIndex(lessonId);
        var questions = questionRepository.findByLessonId(lessonId);
        return buildDetail(lesson, vocab, questions);
    }

    @Override
    public int submitTest(Long lessonId, Long userId, TestSubmitRequest request) {
        var questions = questionRepository.findByLessonId(lessonId);
        int correct = 0;
        for (var q : questions) {
            String selected = request.getAnswers().get(q.getId());
            if (q.getCorrectOption().equalsIgnoreCase(selected)) correct++;
        }
        return correct;
    }

    @Override
    public void askTeacher(Long lessonId, Long userId, String question) {
        TeacherQuestion tq = TeacherQuestion.builder()
                .userId(userId)
                .lessonId(lessonId)
                .question(question)
                .createdAt(LocalDateTime.now())
                .build();
        teacherQuestionRepository.save(tq);
    }

    private LessonDetailDto buildDetail(
            uz.sevenEdu.teacherBot.lesson.entity.Lesson lesson,
            List<uz.sevenEdu.teacherBot.lesson.entity.Vocabulary> vocab,
            List<uz.sevenEdu.teacherBot.lesson.entity.Question> questions) {

        return LessonDetailDto.builder()
                .id(lesson.getId())
                .courseId(lesson.getCourse() != null ? lesson.getCourse().getId() : null)
                .title(lesson.getName())
                .orderIndex(lesson.getOrderIndex())
                .durationSec(lesson.getDurationSec())
                .vocabulary(vocab.stream().map(v -> LessonDetailDto.VocabDto.builder()
                        .id(v.getId()).phraseUz(v.getTranslationUz()).phraseEn(v.getTranslationTarget()).build()
                ).toList())
                .questions(questions.stream().map(q -> LessonDetailDto.QuestionDto.builder()
                        .id(q.getId()).questionText(q.getQuestionText())
                        .optionA(q.getOptionA()).optionB(q.getOptionB()).optionC(q.getOptionC()).build()
                ).toList())
                .build();
    }
}
