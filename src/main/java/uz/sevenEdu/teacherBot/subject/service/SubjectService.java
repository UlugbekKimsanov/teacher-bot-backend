package uz.sevenEdu.teacherBot.subject.service;

import uz.sevenEdu.teacherBot.subject.dto.SubjectDto;

import java.util.List;

public interface SubjectService {
    List<SubjectDto> getAllSubjects();
    SubjectDto getSubjectById(Long subjectId);
}
