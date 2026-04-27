package uz.sevenEdu.teacherBot.news.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.sevenEdu.teacherBot.news.dto.NewsDto;
import uz.sevenEdu.teacherBot.news.repository.NewsRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsRepository newsRepository;

    @Override
    public List<NewsDto> getLatestNews(int limit) {
        return newsRepository.findLatest(limit).stream()
                .map(n -> NewsDto.builder()
                        .id(n.getId())
                        .name(n.getName())
                        .imageId(n.getImageId())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
    }
}
