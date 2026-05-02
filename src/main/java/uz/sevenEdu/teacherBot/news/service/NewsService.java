package uz.sevenEdu.teacherBot.news.service;

import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.news.dto.NewsDto;

public interface NewsService {
    Flux<NewsDto> getLatestNews(int limit);
}
