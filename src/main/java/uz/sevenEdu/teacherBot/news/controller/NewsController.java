package uz.sevenEdu.teacherBot.news.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.common.response.ApiResponse;
import uz.sevenEdu.teacherBot.news.dto.NewsDto;
import uz.sevenEdu.teacherBot.news.service.NewsService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsService newsService;

    @GetMapping
    public Mono<ApiResponse<List<NewsDto>>> getLatestNews(@RequestParam(defaultValue = "10") int limit) {
        return newsService.getLatestNews(limit).collectList().map(ApiResponse::ok);
    }
}
