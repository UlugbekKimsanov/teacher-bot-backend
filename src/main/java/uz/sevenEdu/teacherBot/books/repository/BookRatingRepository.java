package uz.sevenEdu.teacherBot.books.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.entity.BookRating;

public interface BookRatingRepository extends ReactiveCrudRepository<BookRating, Long> {

    Mono<BookRating> findByUserIdAndBookId(Long userId, Long bookId);

    @Query("SELECT COUNT(*) FROM book_ratings WHERE book_id = :bookId")
    Mono<Long> countByBookId(Long bookId);

    @Query("SELECT COALESCE(SUM(rating), 0) FROM book_ratings WHERE book_id = :bookId")
    Mono<Long> sumByBookId(Long bookId);
}
