package uz.sevenEdu.teacherBot.books.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.dto.BooksDto;
import uz.sevenEdu.teacherBot.books.service.BooksService;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BooksController {
    private final BooksService booksService;

    @GetMapping
    public Flux<BooksDto> getAll() {
        return booksService.getAllBooks();
    }

    @GetMapping("/{id}")
    public Mono<BooksDto> getById(@PathVariable Long id) {
        return booksService.getBookById(id);
    }

    @PostMapping
    public Mono<BooksDto> create(@RequestBody BooksDto dto) {
        return booksService.createBook(dto);
    }

    @PutMapping("/{id}")
    public Mono<BooksDto> update(@PathVariable Long id, @RequestBody BooksDto dto) {
        return booksService.updateBook(id, dto);
    }

    @DeleteMapping("/{id}")
    public Mono<BooksDto> delete(@PathVariable Long id) {
        return booksService.deleteBookAndReturn(id);
    }
}