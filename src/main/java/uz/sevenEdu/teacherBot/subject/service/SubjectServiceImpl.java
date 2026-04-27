package uz.sevenEdu.teacherBot.subject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.sevenEdu.teacherBot.common.exception.NotFoundException;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;
import uz.sevenEdu.teacherBot.course.repository.CourseRepository;
import uz.sevenEdu.teacherBot.subject.dto.SubjectDto;
import uz.sevenEdu.teacherBot.subject.entity.Subject;
import uz.sevenEdu.teacherBot.subject.repository.SubjectRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;

    @Override
    public List<SubjectDto> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(this::enrichWithCourses)
                .toList();
    }

    @Override
    public SubjectDto getSubjectById(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new NotFoundException("Fan topilmadi"));
        return enrichWithCourses(subject);
    }

    private SubjectDto enrichWithCourses(Subject subject) {
        var courses = courseRepository.findBySubjectId(subject.getId()).stream()
                .map(course -> CourseDto.builder()
                        .id(course.getId())
                        .name(course.getName())
                        .build())
                .toList();
        return SubjectDto.builder()
                .id(subject.getId())
                .name(subject.getName())
                .imageId(subject.getImageId())
                .courses(courses)
                .build();
    }
}
