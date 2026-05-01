package uz.sevenEdu.teacherBot.books.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import uz.sevenEdu.teacherBot.books.entity.Books;

@Repository
public interface BooksRepository extends ReactiveCrudRepository<Books, Long> {

}