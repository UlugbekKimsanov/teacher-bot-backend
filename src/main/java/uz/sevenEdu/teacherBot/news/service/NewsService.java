package uz.sevenEdu.teacherBot.news.service;

import uz.sevenEdu.teacherBot.news.dto.NewsDto;

import java.util.List;

public interface NewsService {
    List<NewsDto> getLatestNews(int limit);
}
