package uz.sevenEdu.teacherBot.news.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.news.dto.NewsDto;
import uz.sevenEdu.teacherBot.news.repository.NewsRepository;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {
    private final NewsRepository newsRepository;

    @Override
    public Flux<NewsDto> getLatestNews(int limit) {
        return newsRepository.findLatest(limit)
                .map(n -> NewsDto.builder()
                        .id(n.getId()).name(n.getName())
                        .imageId(n.getImageId()).createdAt(n.getCreatedAt()).build());
    }
}
