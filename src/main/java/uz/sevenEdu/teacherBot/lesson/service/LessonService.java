package uz.sevenEdu.teacherBot.lesson.service;

import uz.sevenEdu.teacherBot.lesson.dto.LessonDetailDto;
import uz.sevenEdu.teacherBot.lesson.dto.TestSubmitRequest;

import java.util.List;

public interface LessonService {
    List<LessonDetailDto> getLessonsByCourse(Long courseId, Long userId);
    LessonDetailDto getLessonById(Long lessonId, Long userId);
    int submitTest(Long lessonId, Long userId, TestSubmitRequest request);
    void askTeacher(Long lessonId, Long userId, String question);
}
