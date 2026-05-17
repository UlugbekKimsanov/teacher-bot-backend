package uz.sevenEdu.teacherBot.books.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import uz.sevenEdu.teacherBot.books.entity.Books;

@Repository
public interface BooksRepository extends ReactiveCrudRepository<Books, Long> {
    Flux<Books> findByCategory(String category);
}
