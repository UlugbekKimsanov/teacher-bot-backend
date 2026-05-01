package uz.sevenEdu.teacherBot.books.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.dto.BooksDto;
import uz.sevenEdu.teacherBot.books.entity.Books;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BooksService {
    private final BooksRepository booksRepository;

    public Mono<BooksDto> createBook(BooksDto dto) {
        Books book = mapToEntity(dto);
        book.setCreatedAt(LocalDateTime.now());

        return booksRepository.save(book)
                .map(this::mapToDto);
    }

    public Flux<BooksDto> getAllBooks() {
       return booksRepository.findAll().map(this::mapToDto);
    }

    public Mono<BooksDto> getBookById(Long id) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .map(this::mapToDto);
    }

    public Mono<BooksDto> updateBook(Long id, BooksDto dto) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(existingBook -> {
                    existingBook.setTitle(dto.getTitle());
                    existingBook.setAuthor(dto.getAuthor());
                    existingBook.setCategory(dto.getCategory());
                    existingBook.setDescription(dto.getDescription());
                    existingBook.setPrice(dto.getPrice());
                    existingBook.setImageId(dto.getImageId());
                    existingBook.setFileId(dto.getFileId());
                    existingBook.setUpdatedAt(LocalDateTime.now());

                    return booksRepository.save(existingBook);
                })
                .map(this::mapToDto);
    }

    public Mono<BooksDto> deleteBookAndReturn(Long id) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("O'chirish uchun kitob topilmadi")))
                .flatMap(book ->
                        booksRepository.deleteById(id)
                                .then(Mono.just(mapToDto(book)))
                );
    }

    private BooksDto mapToDto(Books entity) {
        return BooksDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .author(entity.getAuthor())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .imageId(entity.getImageId())
                .fileId(entity.getFileId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Books mapToEntity(BooksDto dto) {
        return Books.builder()
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .imageId(dto.getImageId())
                .fileId(dto.getFileId())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}