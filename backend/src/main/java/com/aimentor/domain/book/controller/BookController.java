package com.aimentor.domain.book.controller;

import com.aimentor.common.api.ApiResponse;
import com.aimentor.common.security.AuthenticatedUser;
import com.aimentor.domain.book.dto.request.BookUpsertRequest;
import com.aimentor.domain.book.dto.response.BookPageResponse;
import com.aimentor.domain.book.dto.response.BookResponse;
import com.aimentor.domain.book.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes book search endpoints for users and CRUD endpoints for admins.
 */
@Validated
@RestController
@RequestMapping({"/api/books", "/api/v1/books"})
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ApiResponse<BookPageResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(bookService.list(keyword, page, size));
    }

    @GetMapping("/{bookId}")
    public ApiResponse<BookResponse> get(@PathVariable Long bookId) {
        return ApiResponse.success(bookService.get(bookId));
    }

    @PostMapping
    public ApiResponse<BookResponse> create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody BookUpsertRequest request
    ) {
        return ApiResponse.success(bookService.create(authenticatedUser.role(), request));
    }

    @PutMapping("/{bookId}")
    public ApiResponse<BookResponse> update(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long bookId,
            @Valid @RequestBody BookUpsertRequest request
    ) {
        return ApiResponse.success(bookService.update(authenticatedUser.role(), bookId, request));
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long bookId
    ) {
        bookService.delete(authenticatedUser.role(), bookId);
        return ApiResponse.success();
    }
}
