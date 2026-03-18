package com.aimentor.domain.book.service;

import com.aimentor.common.exception.ApiException;
import com.aimentor.domain.book.dto.request.BookUpsertRequest;
import com.aimentor.domain.book.dto.response.BookPageResponse;
import com.aimentor.domain.book.dto.response.BookResponse;
import com.aimentor.domain.book.entity.Book;
import com.aimentor.domain.book.repository.BookRepository;
import com.aimentor.domain.user.entity.Role;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles book queries for users and CRUD operations for admins.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public BookPageResponse list(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Book> bookPage = (keyword == null || keyword.isBlank())
                ? bookRepository.findAll(pageable)
                : bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrPublisherContainingIgnoreCase(
                        keyword,
                        keyword,
                        keyword,
                        pageable
                );

        return new BookPageResponse(
                bookPage.getContent().stream().map(this::toResponse).toList(),
                bookPage.getNumber(),
                bookPage.getSize(),
                bookPage.getTotalElements(),
                bookPage.getTotalPages()
        );
    }

    public BookResponse get(Long bookId) {
        return toResponse(getBook(bookId));
    }

    @Transactional
    public BookResponse create(Role role, BookUpsertRequest request) {
        ensureAdmin(role);
        Book savedBook = bookRepository.save(Book.builder()
                .title(request.title())
                .author(request.author())
                .publisher(request.publisher())
                .price(request.price())
                .stock(request.stock())
                .coverUrl(request.coverUrl())
                .description(request.description())
                .build());
        return toResponse(savedBook);
    }

    @Transactional
    public BookResponse update(Role role, Long bookId, BookUpsertRequest request) {
        ensureAdmin(role);
        Book book = getBook(bookId);
        book.update(
                request.title(),
                request.author(),
                request.publisher(),
                request.price(),
                request.stock(),
                request.coverUrl(),
                request.description()
        );
        return toResponse(book);
    }

    @Transactional
    public void delete(Role role, Long bookId) {
        ensureAdmin(role);
        bookRepository.delete(getBook(bookId));
    }

    private Book getBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND", "Book not found."));
    }

    private void ensureAdmin(Role role) {
        if (!Objects.equals(role, Role.ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "Admin permission is required.");
        }
    }

    private BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getPrice(),
                book.getStock(),
                book.getCoverUrl(),
                book.getDescription(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}
