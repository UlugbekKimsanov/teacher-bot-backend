package uz.sevenEdu.teacherBot.lesson.dto;

import lombok.Data;

@Data
public class VocabSubmitRequest {
    private Integer score; // how many vocab words the user got correct
}
