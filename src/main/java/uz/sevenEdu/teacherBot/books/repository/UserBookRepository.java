package uz.sevenEdu.teacherBot.books.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.entity.UserBook;

@Repository
public interface UserBookRepository extends ReactiveCrudRepository<UserBook, Long> {
    Mono<Boolean> existsByUserIdAndBookId(Long userId, Long bookId);
    Mono<UserBook> findByUserIdAndBookId(Long userId, Long bookId);
    Flux<UserBook> findByUserId(Long userId);
    Flux<UserBook> findByUserIdAndIsActive(Long userId, Boolean isActive);

    @Modifying
    @Query("UPDATE user_books SET is_active = :active WHERE user_id = :userId AND book_id = :bookId")
    Mono<Void> setActive(Long userId, Long bookId, boolean active);

    @Modifying
    @Query("UPDATE user_books SET read_page = :readPage, total_pages = :totalPages WHERE user_id = :userId AND book_id = :bookId")
    Mono<Void> updateProgress(Long userId, Long bookId, int readPage, int totalPages);
}
