package uz.sevenEdu.teacherBot.subject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.subject.dto.SubjectDto;
import uz.sevenEdu.teacherBot.subject.repository.SubjectRepository;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;

    @Override
    public Flux<SubjectDto> getAllSubjects() {
        return subjectRepository.findAll().flatMap(this::enrichWithCourses);
    }

    @Override
    public Mono<SubjectDto> getSubjectById(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .switchIfEmpty(Mono.error(new NotFoundException("Fan topilmadi")))
                .flatMap(this::enrichWithCourses);
    }

    private Mono<SubjectDto> enrichWithCourses(uz.sevenEdu.teacherBot.subject.entity.Subject subject) {
        return courseRepository.findBySubjectId(subject.getId())
                .map(course -> CourseDto.builder().id(course.getId()).name(course.getName()).build())
                .collectList()
                .map(courses -> SubjectDto.builder()
                        .id(subject.getId()).name(subject.getName())
                        .imageId(subject.getImageId()).courses(courses).build());
    }
}
