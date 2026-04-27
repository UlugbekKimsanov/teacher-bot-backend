package uz.sevenEdu.teacherBot.subject.dto;

import lombok.Builder;
import lombok.Data;
import uz.sevenEdu.teacherBot.course.dto.CourseDto;

import java.util.List;

@Data
@Builder
public class SubjectDto {
    private Long id;
    private String name;
    private Long imageId;
    private List<CourseDto> courses;
}
