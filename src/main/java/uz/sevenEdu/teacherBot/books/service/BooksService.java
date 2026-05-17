package uz.sevenEdu.teacherBot.books.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uz.sevenEdu.teacherBot.books.dto.BooksDto;
import uz.sevenEdu.teacherBot.books.entity.Books;
import uz.sevenEdu.teacherBot.books.entity.UserBook;
import uz.sevenEdu.teacherBot.books.repository.BooksRepository;
import uz.sevenEdu.teacherBot.books.repository.UserBookRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BooksService {
    private final BooksRepository booksRepository;
    private final UserBookRepository userBookRepository;
    private static final ObjectMapper mapper = new ObjectMapper();

    public Flux<BooksDto> getBooksByCategory(String category, Long userId) {
        Flux<Books> booksFlux = booksRepository.findByCategory(category);
        if (userId == null) {
            return booksFlux.map(b -> mapToDto(b, false));
        }
        return booksFlux.collectList().flatMapMany(books -> {
            List<Long> bookIds = books.stream().map(Books::getId).toList();
            return userBookRepository.findByUserId(userId).collectList().flatMapMany(userBooks -> {
                Set<Long> purchasedIds = userBooks.stream()
                        .map(UserBook::getBookId).collect(Collectors.toSet());
                return Flux.fromIterable(books)
                        .map(b -> mapToDto(b, purchasedIds.contains(b.getId())));
            });
        });
    }

    public Mono<BooksDto> getBookById(Long id, Long userId) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(book -> {
                    if (userId == null) return Mono.just(mapToDto(book, false));
                    return userBookRepository.existsByUserIdAndBookId(userId, id)
                            .map(purchased -> mapToDto(book, purchased));
                });
    }

    public Mono<Void> purchaseBook(Long userId, Long bookId) {
        return userBookRepository.existsByUserIdAndBookId(userId, bookId)
                .flatMap(exists -> {
                    if (exists) return Mono.empty();
                    return userBookRepository.save(UserBook.builder()
                            .userId(userId).bookId(bookId)
                            .paymentMethod("card")
                            .purchasedAt(LocalDateTime.now())
                            .build()).then();
                });
    }

    public Flux<BooksDto> getMyBooks(Long userId) {
        return userBookRepository.findByUserId(userId).collectList()
                .flatMapMany(userBooks -> {
                    if (userBooks.isEmpty()) return Flux.empty();
                    List<Long> bookIds = userBooks.stream()
                            .map(UserBook::getBookId).toList();
                    return booksRepository.findAllById(bookIds)
                            .map(b -> mapToDto(b, true));
                });
    }

    // ── Admin CRUD (keep existing) ──────────────────────────────────

    public Mono<BooksDto> createBook(BooksDto dto) {
        Books book = mapToEntity(dto);
        book.setCreatedAt(LocalDateTime.now());
        return booksRepository.save(book).map(b -> mapToDto(b, false));
    }

    public Flux<BooksDto> getAllBooks() {
        return booksRepository.findAll().map(b -> mapToDto(b, false));
    }

    public Mono<BooksDto> updateBook(Long id, BooksDto dto) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(existing -> {
                    existing.setTitle(dto.getTitle());
                    existing.setAuthor(dto.getAuthor());
                    existing.setCategory(dto.getCategory());
                    existing.setDescription(dto.getDescription());
                    existing.setPrice(dto.getPrice());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return booksRepository.save(existing);
                })
                .map(b -> mapToDto(b, false));
    }

    public Mono<BooksDto> deleteBookAndReturn(Long id) {
        return booksRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Kitob topilmadi")))
                .flatMap(book -> booksRepository.deleteById(id)
                        .then(Mono.just(mapToDto(book, false))));
    }

    // ── Mappers ─────────────────────────────────────────────────────

    private BooksDto mapToDto(Books entity, boolean isPurchased) {
        List<Integer> coverColors = new ArrayList<>();
        if (entity.getCoverColor1() != null) coverColors.add(entity.getCoverColor1());
        if (entity.getCoverColor2() != null) coverColors.add(entity.getCoverColor2());
        if (coverColors.isEmpty()) {
            coverColors.add(0xFF2E9E6E);
            coverColors.add(0xFF1F7A55);
        }

        List<String> previewPages = new ArrayList<>();
        if (entity.getPreviewPages() != null && !entity.getPreviewPages().isBlank()) {
            try {
                previewPages = mapper.readValue(entity.getPreviewPages(),
                        new TypeReference<List<String>>() {});
            } catch (Exception ignored) {}
        }

        return BooksDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .author(entity.getAuthor())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .priceLabel(entity.getPriceLabel())
                .isFree(entity.getIsFree() != null && entity.getIsFree())
                .emoji(entity.getEmoji() != null ? entity.getEmoji() : "\uD83D\uDCDA")
                .coverColors(coverColors)
                .pages(entity.getPages())
                .pageCount(entity.getPageCount())
                .format(entity.getFormat())
                .rating(entity.getRating())
                .reviewCount(entity.getReviewCount())
                .language(entity.getLanguage())
                .level(entity.getLevel())
                .previewPages(previewPages)
                .isPurchased(isPurchased)
                .imageId(entity.getImageId())
                .fileId(entity.getFileId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Books mapToEntity(BooksDto dto) {
        List<Integer> colors = dto.getCoverColors();
        String previewJson = null;
        if (dto.getPreviewPages() != null) {
            try { previewJson = mapper.writeValueAsString(dto.getPreviewPages()); }
            catch (Exception ignored) {}
        }
        return Books.builder()
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .category(dto.getCategory())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .priceLabel(dto.getPriceLabel())
                .isFree(dto.getIsFree())
                .emoji(dto.getEmoji())
                .coverColor1(colors != null && !colors.isEmpty() ? colors.get(0) : null)
                .coverColor2(colors != null && colors.size() > 1 ? colors.get(1) : null)
                .pages(dto.getPages())
                .pageCount(dto.getPageCount())
                .format(dto.getFormat())
                .rating(dto.getRating())
                .reviewCount(dto.getReviewCount())
                .language(dto.getLanguage())
                .level(dto.getLevel())
                .previewPages(previewJson)
                .imageId(dto.getImageId())
                .fileId(dto.getFileId())
                .build();
    }
}
