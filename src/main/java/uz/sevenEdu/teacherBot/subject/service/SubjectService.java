package uz.sevenEdu.teacherBot.subject.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.subject.dto.SubjectDto;

public interface SubjectService {
    Flux<SubjectDto> getAllSubjects();
    Mono<SubjectDto> getSubjectById(Long subjectId);
}
