package uz.sevenEdu.teacherBot.books.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.entity.UserBook;

@Repository
public interface UserBookRepository extends ReactiveCrudRepository<UserBook, Long> {
    Mono<Boolean> existsByUserIdAndBookId(Long userId, Long bookId);
    Flux<UserBook> findByUserId(Long userId);
}
