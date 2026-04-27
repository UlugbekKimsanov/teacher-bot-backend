package uz.sevenEdu.teacherBot.file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import uz.sevenEdu.teacherBot.file.repository.FileRepository;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileRepository fileRepository;
}
