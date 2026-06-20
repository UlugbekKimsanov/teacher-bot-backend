package uz.sevenEdu.teacherBot.breakmusic.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.breakmusic.entity.BreakGroup;
import uz.sevenEdu.teacherBot.breakmusic.repository.BreakGroupRepository;
import uz.sevenEdu.teacherBot.breakmusic.repository.BreakTrackRepository;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.common.service.FileStorageService;

import java.util.List;

/** Mobil: tanaffus guruhlari + musiqalar (public URL bilan). */
@RestController
@RequestMapping("/api/v1/break-groups")
@RequiredArgsConstructor
public class BreakMusicController {

    private final BreakGroupRepository breakGroupRepository;
    private final BreakTrackRepository breakTrackRepository;
    private final FileStorageService fileStorageService;

    @GetMapping
    public Mono<ApiResponse<List<BreakGroup>>> getGroups() {
        return breakGroupRepository.findAllByOrderByOrderIndexAsc()
                .flatMap(group -> breakTrackRepository.findByGroupIdOrderById(group.getId())
                        .map(t -> {
                            t.setFilePath(fileStorageService.toPublicUrl(t.getFilePath()));
                            return t;
                        })
                        .collectList()
                        .map(tracks -> {
                            group.setBackgroundImage(fileStorageService.toPublicUrl(group.getBackgroundImage()));
                            group.setTracks(tracks);
                            return group;
                        }))
                .collectList()
                .map(ApiResponse::ok);
    }
}
